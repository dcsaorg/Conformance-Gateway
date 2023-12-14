package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;
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
public class UC5_Shipper_CancelUpdateToShippingInstructionsAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC5_Shipper_CancelUpdateToShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC5", 200);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC5: Cancel update to shipping instructions the document reference %s".formatted(
      getDspSupplier().get().shippingInstructionsReference()
    ));
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("documentReference", getDspSupplier().get().shippingInstructionsReference());
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
        var dsp = getDspSupplier().get();
        var sir = dsp.shippingInstructionsReference() != null ? dsp.shippingInstructionsReference() : "<DSP MISSING SI REFERENCE>";
        var siStatus = Objects.requireNonNullElse(dsp.shippingInstructionsStatus(), ShippingInstructionsStatus.SI_RECEIVED);
        Stream<ActionCheck> primaryExchangeChecks =
          Stream.of(
            new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "PATCH"),
            new UrlPathCheck(EblRole::isShipper, getMatchedExchangeUuid(), "/v3/shipping-instructions/%s".formatted(sir)),
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
            // TODO: Add Carrier Ref Status Payload response check
            // TODO: Add Shipper content conformance check
            new JsonSchemaCheck(
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator),
            new JsonSchemaCheck(
                EblRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator));
        return Stream.concat(
          primaryExchangeChecks,
          Stream.concat(
            Stream.concat(
              EBLChecks.siRefSIR(getMatchedExchangeUuid(), sir),
              EBLChecks.siRefStatusChecks(
                getMatchedExchangeUuid(),
                siStatus,
                ShippingInstructionsStatus.SI_UPDATE_CANCELLED
              )),
            getSINotificationChecks(
              expectedApiVersion,
              notificationSchemaValidator,
              siStatus,
              ShippingInstructionsStatus.SI_UPDATE_CANCELLED)));
      }
    };
  }
}
