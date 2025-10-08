package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EblChecks;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

@Getter
@Slf4j
public class UC3ShipperSubmitUpdatedShippingInstructionsAction extends StateChangingSIAction {
  private final ShippingInstructionsStatus expectedSiStatus;
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final boolean useBothRef;

  public UC3ShipperSubmitUpdatedShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      ShippingInstructionsStatus expectedSiStatus,
      boolean useBothRef,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator,
      boolean isWithNotifications) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC3", 202, isWithNotifications);
    this.useBothRef = useBothRef;
    this.expectedSiStatus = expectedSiStatus;
    this.requestSchemaValidator = requestSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of(
            "REFERENCE",
            this.useBothRef
                ? String.format(
                    "either the TD reference (%s) or the SI reference (%s)",
                    getDSP().transportDocumentReference(), getDSP().shippingInstructionsReference())
                : "document reference " + getDSP().shippingInstructionsReference()),
        "prompt-shipper-uc3.md",
        "prompt-shipper-refresh-complete.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    var documentReference =
        this.useBothRef ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference();
    if (documentReference == null) {
      throw new IllegalStateException("Missing document reference for use-case 3");
    }
    return super.asJsonNode()
        .put("sir", dsp.shippingInstructionsReference())
        .put("documentReference", documentReference);
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

        Stream<ActionCheck> primaryExchangeChecks =
            Stream.of(
                new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "PUT"),
                new UrlPathCheck(
                    EblRole::isShipper,
                    getMatchedExchangeUuid(),
                    buildFullUris(
                        "/v3/shipping-instructions/",
                        dsp.shippingInstructionsReference(),
                        dsp.transportDocumentReference())),
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
                EblChecks.siRequestContentChecks(
                    getMatchedExchangeUuid(),
                    expectedApiVersion,
                    ScenarioType.valueOf(getDspSupplier().get().scenarioType())));
        return Stream.concat(
            primaryExchangeChecks,
            getSINotificationChecks(
                getMatchedNotificationExchangeUuid(),
                expectedApiVersion,
                notificationSchemaValidator,
                expectedSiStatus,
                ShippingInstructionsStatus.SI_UPDATE_RECEIVED,
                EblChecks.sirInNotificationMustMatchDSP(getDspSupplier())));
      }
    };
  }
}
