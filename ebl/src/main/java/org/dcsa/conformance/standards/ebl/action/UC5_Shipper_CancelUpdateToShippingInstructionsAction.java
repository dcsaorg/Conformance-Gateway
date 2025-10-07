package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EblChecks;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

@Getter
@Slf4j
public class UC5_Shipper_CancelUpdateToShippingInstructionsAction extends StateChangingSIAction {
  private final ShippingInstructionsStatus expectedSIStatus;
  private final boolean useTDRef;
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC5_Shipper_CancelUpdateToShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      ShippingInstructionsStatus expectedSIStatus,
      boolean useTDRef,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator,
      boolean isWithNotifications) {
    super(shipperPartyName, carrierPartyName, previousAction, useTDRef ? "UC5 [TDR]" : "UC5", 202, isWithNotifications);
    this.expectedSIStatus = expectedSIStatus;
    this.useTDRef = useTDRef;
    this.requestSchemaValidator = requestSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    var dsp = getDSP();
    return getMarkdownHumanReadablePrompt(
        Map.of(
            "REFERENCE",
            this.useTDRef ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference()),
        "prompt-shipper-uc5.md",
        "prompt-shipper-refresh-complete.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDSP();
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
        var documentReference =
            useTDRef
                ? Objects.requireNonNullElse(
                    dsp.transportDocumentReference(), "<DSP MISSING TD REFERENCE>")
                : Objects.requireNonNullElse(
                    dsp.shippingInstructionsReference(), "<DSP MISSING SI REFERENCE>");
        Stream<ActionCheck> primaryExchangeChecks =
            Stream.of(
                new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "PATCH"),
                new UrlPathCheck(
                    EblRole::isShipper,
                    getMatchedExchangeUuid(),
                    "/v3/shipping-instructions/%s".formatted(documentReference)),
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
                    requestSchemaValidator));
        return Stream.concat(
            primaryExchangeChecks,
            getSINotificationChecks(
                getMatchedNotificationExchangeUuid(),
                expectedApiVersion,
                notificationSchemaValidator,
                expectedSIStatus,
                ShippingInstructionsStatus.SI_UPDATE_CANCELLED,
                EblChecks.sirInNotificationMustMatchDSP(getDspSupplier())));
      }
    };
  }
}
