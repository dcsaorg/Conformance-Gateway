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
  private final boolean useTDRef;
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC5_Shipper_CancelUpdateToShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      boolean useTDRef,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, useTDRef ? "UC5 [TDR]" : "UC5", 200);
    this.useTDRef = useTDRef;
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    var dsp = getDspSupplier().get();
    return ("UC5: Cancel update to shipping instructions the document reference %s".formatted(
      useTDRef ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference()
    ));
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    return super.asJsonNode()
      .put("documentReference", useTDRef ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference());
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
        var documentReference = useTDRef
          ? Objects.requireNonNullElse(dsp.transportDocumentReference(), "<DSP MISSING TD REFERENCE>")
          : Objects.requireNonNullElse(dsp.shippingInstructionsReference(), "<DSP MISSING SI REFERENCE>");
        var siStatus = Objects.requireNonNullElse(dsp.shippingInstructionsStatus(), ShippingInstructionsStatus.SI_RECEIVED);
        Stream<ActionCheck> primaryExchangeChecks =
          Stream.of(
            new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "PATCH"),
            new UrlPathCheck(EblRole::isShipper, getMatchedExchangeUuid(), "/v3/shipping-instructions/%s".formatted(documentReference)),
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
                responseSchemaValidator));
        return Stream.concat(
          primaryExchangeChecks,
          Stream.concat(
            EBLChecks.siRefStatusContentChecks(
              getMatchedExchangeUuid(),
              expectedApiVersion,
              siStatus,
              ShippingInstructionsStatus.SI_UPDATE_CANCELLED,
              EBLChecks.sirInRefStatusMustMatchDSP(getDspSupplier())
            ),
            getSINotificationChecks(
              getMatchedNotificationExchangeUuid(),
              expectedApiVersion,
              notificationSchemaValidator,
              siStatus,
              ShippingInstructionsStatus.SI_UPDATE_CANCELLED,
              EBLChecks.sirInNotificationMustMatchDSP(getDspSupplier()))));
      }
    };
  }
}
