package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.*;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class EblAction extends ConformanceAction {
  protected final int expectedStatus;
  private final OverwritingReference<DynamicScenarioParameters> dspReference;

  public EblAction(
      String sourcePartyName,
      String targetPartyName,
      EblAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
    this.dspReference =
        previousAction == null
            ? new OverwritingReference<>(
                null, new DynamicScenarioParameters(null, null, null, null, null))
            : new OverwritingReference<>(previousAction.dspReference, null);
  }

  @Override
  public void reset() {
    super.reset();
    if (previousAction != null) {
      this.dspReference.set(null);
    }
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (dspReference.hasCurrentValue()) {
      jsonState.set("currentDsp", dspReference.get().toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode dspNode = jsonState.get("currentDsp");
    if (dspNode != null) {
      dspReference.set(DynamicScenarioParameters.fromJson(dspNode));
    }
  }

  protected EblAction getPreviousEblAction() {
    return (EblAction) previousAction;
  }

  protected Consumer<CarrierScenarioParameters> getCspConsumer() {
    return getPreviousEblAction().getCspConsumer();
  }

  protected Supplier<CarrierScenarioParameters> getCspSupplier() {
    return getPreviousEblAction().getCspSupplier();
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dspReference::get;
  }

  protected void updateDSPFromResponsePayload(ConformanceExchange exchange) {
    DynamicScenarioParameters dsp = dspReference.get();

    JsonNode responseJsonNode = exchange.getResponse().message().body().getJsonBody();
    var newShippingInstructionsReference =
        responseJsonNode.path("shippingInstructionsReference").asText(null);
    var newTransportDocumentReference =
        responseJsonNode.path("transportDocumentReference").asText(null);
    var newShippingInstructionsStatus =
        parseShippingInstructionsStatus(
            responseJsonNode.path("shippingInstructionsStatus").asText(null));
    var newUpdatedShippingInstructionsStatus =
        parseShippingInstructionsStatus(
            responseJsonNode.path("updatedShippingInstructionsStatus").asText(null));
    var newTransportDocumentStatus =
        parseTransportDocumentStatus(responseJsonNode.path("transportDocumentStatus").asText(null));

    var updatedDsp = dsp;
    updatedDsp =
        updateIfNotNull(
            updatedDsp,
            newShippingInstructionsReference,
            updatedDsp::withShippingInstructionsReference);
    updatedDsp =
        updateIfNotNull(
            updatedDsp, newTransportDocumentReference, updatedDsp::withTransportDocumentReference);
    updatedDsp =
        updateIfNotNull(
            updatedDsp, newShippingInstructionsStatus, updatedDsp::withShippingInstructionsStatus);
    updatedDsp =
        updateIfNotNull(
            updatedDsp,
            newUpdatedShippingInstructionsStatus,
            updatedDsp::withUpdatedShippingInstructionsStatus);
    updatedDsp =
        updateIfNotNull(
            updatedDsp, newTransportDocumentStatus, updatedDsp::withTransportDocumentStatus);

    if (!dsp.equals(updatedDsp)) {
      dspReference.set(updatedDsp);
    }
  }

  private static ShippingInstructionsStatus parseShippingInstructionsStatus(String v) {
    if (v == null) {
      return null;
    }
    try {
      return ShippingInstructionsStatus.fromWireName(v);
    } catch (IllegalArgumentException e) {
      // Do not assume conformant payload.
      return null;
    }
  }

  private static TransportDocumentStatus parseTransportDocumentStatus(String v) {
    if (v == null) {
      return null;
    }
    try {
      return TransportDocumentStatus.fromWireName(v);
    } catch (IllegalArgumentException e) {
      // Do not assume conformant payload.
      return null;
    }
  }

  private <T> DynamicScenarioParameters updateIfNotNull(
      DynamicScenarioParameters dsp, T value, Function<T, DynamicScenarioParameters> with) {
    if (value == null) {
      return dsp;
    }
    return with.apply(value);
  }

  protected Stream<ActionCheck> getSINotificationChecks(
      String expectedApiVersion, JsonSchemaValidator notificationSchemaValidator) {
    String titlePrefix = "[Notification]";
    return Stream.of(
            new HttpMethodCheck(
                titlePrefix, EblRole::isCarrier, getMatchedNotificationExchangeUuid(), "POST"),
            new UrlPathCheck(
                titlePrefix,
                EblRole::isCarrier,
                getMatchedNotificationExchangeUuid(),
                "/v3/shipping-instructions-notifications"),
            new ResponseStatusCheck(
                titlePrefix, EblRole::isShipper, getMatchedNotificationExchangeUuid(), 204),
            new ApiHeaderCheck(
                titlePrefix,
                EblRole::isCarrier,
                getMatchedNotificationExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                titlePrefix,
                EblRole::isShipper,
                getMatchedNotificationExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                titlePrefix,
                EblRole::isCarrier,
                getMatchedNotificationExchangeUuid(),
                HttpMessageType.REQUEST,
                notificationSchemaValidator))
        .filter(Objects::nonNull);
  }
}
