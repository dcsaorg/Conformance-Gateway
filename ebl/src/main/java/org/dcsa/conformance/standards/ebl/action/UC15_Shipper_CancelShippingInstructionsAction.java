package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.standards.ebl.checks.EblChecks;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

@Getter
public class UC15_Shipper_CancelShippingInstructionsAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC15_Shipper_CancelShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator,
      boolean isWithNotifications) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC15", 202, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getDSP().shippingInstructionsReference()),
        "prompt-shipper-uc15.md",
        "prompt-shipper-refresh-complete.md");
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
        var documentReference =
            Objects.requireNonNullElse(
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
                ShippingInstructionsStatus.SI_CANCELLED,
                EblChecks.sirInNotificationMustMatchDSP(getDspSupplier())));
      }
    };
  }
}


