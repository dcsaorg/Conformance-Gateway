package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

@Getter
@Slf4j
public class UC1_Shipper_SubmitShippingInstructionsAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC1_Shipper_SubmitShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC1", 202);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("SCENARIO_TYPE", getScenarioType()),
        "prompt-shipper-uc1.md",
        "prompt-shipper-refresh-complete.md");
  }

  private String getScenarioType() {
    return switch (getDspSupplier().get().scenarioType()) {
      case REGULAR_2C_2U_1E ->
          "with 2 Commodities, 2 Utilized transport equipments and 1 Equipment";
      case REGULAR_2C_2U_2E ->
          "with  2 Commodities, 2 Utilized transport equipments and 2 Equipments";
      case REGULAR_NO_COMMODITY_SUBREFERENCE -> "with No Commodity Subreference";
      case REGULAR_SWB_SOC_AND_REFERENCES -> "for Regular SWB and with SOC References";
      case REGULAR_SWB_AMF -> "for Regular SWB with Advance Manifest Filing";
      case DG -> "with Dangerous Goods";
      case REGULAR_SWB -> "for Regular SWB";
      case REGULAR_STRAIGHT_BL -> "for Regular Straight BL";
      case ACTIVE_REEFER -> "with Active Reefer";
      case NON_OPERATING_REEFER -> "with Non-operating Reefer";
      case REGULAR_NEGOTIABLE_BL -> "for Negotiable BL";
      case REGULAR_CLAD -> "for Clad";
    };
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.set("csp", getCspSupplier().get().toJson());
    return jsonNode.put("scenarioType", getDspSupplier().get().scenarioType().name());
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        Stream<ActionCheck> primaryExchangeChecks =
          Stream.of(
            new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "POST"),
            new UrlPathCheck(EblRole::isShipper, getMatchedExchangeUuid(), "/v3/shipping-instructions"),
            new ResponseStatusCheck(
                EblRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
            new ApiHeaderCheck(
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                EblRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator),
            new JsonSchemaCheck(
                EblRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator),
            EBLChecks.siRequestContentChecks(getMatchedExchangeUuid(), expectedApiVersion, getCspSupplier(), getDspSupplier()));
        return Stream.concat(
          primaryExchangeChecks,
          getSINotificationChecks(
            getMatchedNotificationExchangeUuid(),
            expectedApiVersion,
            notificationSchemaValidator,
            ShippingInstructionsStatus.SI_RECEIVED,
            EBLChecks.SIR_REQUIRED_IN_NOTIFICATION)
          );
      }
    };
  }
}
