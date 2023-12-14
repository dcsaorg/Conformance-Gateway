package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
    super(shipperPartyName, carrierPartyName, previousAction, "UC1", 201);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC1: Submit a shipping instructions using the following parameters:");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.set("csp", getCspSupplier().get().toJson());
    return jsonNode;
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
            EBLChecks.siRequestContentChecks(getMatchedExchangeUuid()));
        return Stream.concat(
          primaryExchangeChecks,
          Stream.concat(
              EBLChecks.siRefStatusContentChecks(
                getMatchedExchangeUuid(),
                ShippingInstructionsStatus.SI_RECEIVED,
                EBLChecks.SIR_REQUIRED_IN_REF_STATUS
              ),
              getSINotificationChecks(
                getMatchedNotificationExchangeUuid(),
                expectedApiVersion,
                notificationSchemaValidator,
                ShippingInstructionsStatus.SI_RECEIVED,
                EBLChecks.SIR_REQUIRED_IN_NOTIFICATION))
          );
      }
    };
  }
}
