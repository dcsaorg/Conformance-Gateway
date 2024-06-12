package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.*;

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
                null, new DynamicScenarioParameters(ScenarioType.REGULAR_SWB, null, null, null, null, null, null, null))
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

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dspReference::set;
  }

  protected DynamicScenarioParameters updateDSPFromSIHook(ConformanceExchange exchange, DynamicScenarioParameters dynamicScenarioParameters) {
    return dynamicScenarioParameters;
  }

  protected void updateDSPFromSIResponsePayload(ConformanceExchange exchange) {
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

    updatedDsp = updateDSPFromSIHook(exchange, updatedDsp);

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
    UUID notificationExchangeUuid, String expectedApiVersion, JsonSchemaValidator notificationSchemaValidator, ShippingInstructionsStatus expectedStatus, JsonContentCheck ... extraChecks) {
    return getSINotificationChecks(notificationExchangeUuid, expectedApiVersion, notificationSchemaValidator, expectedStatus, null, extraChecks);
  }

  protected Stream<ActionCheck> getSINotificationChecks(
    UUID notificationExchangeUuid, String expectedApiVersion, JsonSchemaValidator notificationSchemaValidator, ShippingInstructionsStatus expectedStatus, ShippingInstructionsStatus expectedUpdatedStatus, JsonContentCheck ... extraChecks) {
    String titlePrefix = "[Notification]";
    return Stream.of(
            new HttpMethodCheck(
                titlePrefix, EblRole::isCarrier, notificationExchangeUuid, "POST"),
            new UrlPathCheck(
                titlePrefix,
                EblRole::isCarrier,
                notificationExchangeUuid,
                "/v3/shipping-instructions-notifications"),
            new ResponseStatusCheck(
                titlePrefix, EblRole::isShipper, notificationExchangeUuid, 204),
            ApiHeaderCheck.createNotificationCheck(
                titlePrefix,
                EblRole::isCarrier,
                notificationExchangeUuid,
                HttpMessageType.REQUEST,
                expectedApiVersion),
            ApiHeaderCheck.createNotificationCheck(
                titlePrefix,
                EblRole::isShipper,
                notificationExchangeUuid,
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                titlePrefix,
                EblRole::isCarrier,
                notificationExchangeUuid,
                HttpMessageType.REQUEST,
                notificationSchemaValidator),
          EBLChecks.siNotificationContentChecks(notificationExchangeUuid, expectedApiVersion, expectedStatus, expectedUpdatedStatus, extraChecks)
      );
  }

  protected Stream<ActionCheck> getTDNotificationChecks(
    String expectedApiVersion, JsonSchemaValidator notificationSchemaValidator, TransportDocumentStatus transportDocumentStatus) {
    return getTDNotificationChecks(getMatchedNotificationExchangeUuid(), expectedApiVersion, notificationSchemaValidator, transportDocumentStatus);
  }

  protected Stream<ActionCheck> getTDNotificationChecks(
    UUID notificationExchangeUuid, String expectedApiVersion, JsonSchemaValidator notificationSchemaValidator, TransportDocumentStatus transportDocumentStatus) {
    return getTDNotificationChecks(notificationExchangeUuid, expectedApiVersion, notificationSchemaValidator, transportDocumentStatus, true);
  }

  protected Stream<ActionCheck> getTDNotificationChecks(
    UUID notificationExchangeUuid, String expectedApiVersion, JsonSchemaValidator notificationSchemaValidator, TransportDocumentStatus transportDocumentStatus, boolean tdrIsKnown) {
    String titlePrefix = "[Notification]";
    var tdrCheck = tdrIsKnown
      ? EBLChecks.tdrInNotificationMustMatchDSP(getDspSupplier())
      : EBLChecks.TDR_REQUIRED_IN_NOTIFICATION;

    return Stream.of(
      new HttpMethodCheck(
        titlePrefix, EblRole::isCarrier, notificationExchangeUuid, "POST"),
      new UrlPathCheck(
        titlePrefix,
        EblRole::isCarrier,
        notificationExchangeUuid,
        "/v3/transport-document-notifications"),
      new ResponseStatusCheck(
        titlePrefix, EblRole::isShipper, notificationExchangeUuid, 204),
      ApiHeaderCheck.createNotificationCheck(
        titlePrefix,
        EblRole::isCarrier,
        notificationExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion),
      ApiHeaderCheck.createNotificationCheck(
        titlePrefix,
        EblRole::isShipper,
        notificationExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion),
      new JsonSchemaCheck(
        titlePrefix,
        EblRole::isCarrier,
        notificationExchangeUuid,
        HttpMessageType.REQUEST,
        notificationSchemaValidator),
        EBLChecks.tdNotificationContentChecks(notificationExchangeUuid, expectedApiVersion, transportDocumentStatus, tdrCheck)
     );
  }
}
