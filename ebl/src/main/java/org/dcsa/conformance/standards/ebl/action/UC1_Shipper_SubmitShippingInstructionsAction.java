package org.dcsa.conformance.standards.ebl.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EblChecks;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

@Getter
@Slf4j
public class UC1_Shipper_SubmitShippingInstructionsAction extends ShipperNotificationEblAction {
  private static final String CBR_PLACEHOLDER = "{CBR}";
  private static final String DEFAULT_CBR = "BOOKING202507041234567890123456";

  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final ScenarioType standaloneScenarioType;
  private final String standardVersion;

  public UC1_Shipper_SubmitShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator,
      boolean isWithNotifications) {
    this(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        requestSchemaValidator,
        responseSchemaValidator,
        notificationSchemaValidator,
        isWithNotifications,
        null,
        null,
        "UC1");
  }

  public UC1_Shipper_SubmitShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator,
      boolean isWithNotifications,
      ScenarioType standaloneScenarioType,
      String standardVersion) {
    this(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        requestSchemaValidator,
        responseSchemaValidator,
        notificationSchemaValidator,
        isWithNotifications,
        standaloneScenarioType,
        standardVersion,
        "UC1");
  }

  public UC1_Shipper_SubmitShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator,
      boolean isWithNotifications,
      ScenarioType standaloneScenarioType,
      String standardVersion,
      String actionTitle) {
    super(shipperPartyName, carrierPartyName, previousAction, actionTitle, 202, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.standaloneScenarioType = standaloneScenarioType;
    this.standardVersion = standardVersion;
    ensureStandaloneScenarioTypeInitialized();
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("SCENARIO_TYPE", getScenarioType()),
        "prompt-shipper-uc1.md",
        "prompt-shipper-refresh-complete.md");
  }

  private String getScenarioType() {
    return switch (resolveScenarioType()) {
      case REGULAR_2C_1U -> "with 2 Commodities, 1 Utilized transport equipment";
      case REGULAR_2C_2U -> "with  2 Commodities, 2 Utilized transport equipments";
      case REGULAR_NO_COMMODITY_SUBREFERENCE -> "with No Commodity Subreference";
      case REGULAR_SWB_SOC_AND_REFERENCES -> "for Regular SWB and with SOC References";
      case REGULAR_SWB_AMF -> "for Regular SWB with Advance Manifest Filing";
      case DG -> "with Dangerous Goods";
      case REGULAR_SWB -> "for Sea Waybill";
      case REGULAR_STRAIGHT_BL -> "for Straight B/L";
      case ACTIVE_REEFER -> "with Active Reefer";
      case NON_OPERATING_REEFER -> "with Non-operating Reefer";
      case REGULAR_NEGOTIABLE_BL -> "for Negotiable B/L";
      case REGULAR_CLAD ->
          "for Clad (scenario where property `isCarriersAgentAtDestinationRequired` is required)";
    };
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    JsonNode payload =
        previousAction == null ? buildStandalonePayload() : getCarrierPayloadSupplier().get();
    jsonNode.set(CarrierSupplyPayloadAction.CARRIER_PAYLOAD, payload);
    return jsonNode;
  }

  @Override
  public void reset() {
    super.reset();
    ensureStandaloneScenarioTypeInitialized();
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
                new UrlPathCheck(
                    EblRole::isShipper, getMatchedExchangeUuid(), "/v3/shipping-instructions"),
                ResponseStatusCheck.forSuccessfulResponse(
                    EblRole::isCarrier, getMatchedExchangeUuid()),
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
                EblChecks.siRequestContentChecks(
                    getMatchedExchangeUuid(),
                    expectedApiVersion,
                    resolveScenarioType()));
        return Stream.concat(
            primaryExchangeChecks,
            getSINotificationChecks(
                getMatchedNotificationExchangeUuid(),
                expectedApiVersion,
                notificationSchemaValidator,
                ShippingInstructionsStatus.SI_RECEIVED,
                EblChecks.SIR_OR_TDR_REQUIRED_IN_NOTIFICATION));
      }
    };
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    if (previousAction != null) {
      getCarrierPayloadConsumer().accept(OBJECT_MAPPER.createObjectNode());
    }
  }

  private ScenarioType resolveScenarioType() {
    var scenarioTypeName = getDspSupplier().get().scenarioType();
    if (scenarioTypeName != null) {
      return ScenarioType.valueOf(scenarioTypeName);
    }
    if (standaloneScenarioType != null) {
      return standaloneScenarioType;
    }
    return ScenarioType.REGULAR_SWB;
  }

  private void ensureStandaloneScenarioTypeInitialized() {
    if (previousAction != null || standaloneScenarioType == null) {
      return;
    }
    var dsp = getDspSupplier().get();
    if (dsp.scenarioType() == null) {
      getDspConsumer().accept(dsp.withScenarioType(standaloneScenarioType.name()));
    }
  }

  private JsonNode buildStandalonePayload() {
    ScenarioType scenarioType = resolveScenarioType();
    String version = standardVersion == null ? "3.0.0" : standardVersion;
    return JsonToolkit.templateFileToJsonNode(
        "/standards/ebl/messages/" + scenarioType.eblPayload(version),
        Map.of(CBR_PLACEHOLDER, DEFAULT_CBR));
  }
}

