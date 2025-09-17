package org.dcsa.conformance.standards.ebl.action;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.action.BookingAndEblAction;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.ebl.checks.CarrierSiNotificationPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.ebl.checks.CarrierTdNotificationPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.*;

public abstract class EblAction extends BookingAndEblAction {

  protected final Set<Integer> expectedStatus;
  protected ScenarioType scenarioType;

  protected EblAction(
      String sourcePartyName,
      String targetPartyName,
      BookingAndEblAction previousAction,
      String actionTitle,
      int expectedStatus) {
    this(sourcePartyName, targetPartyName, previousAction, actionTitle, Set.of(expectedStatus));
    if (previousAction instanceof EblAction previousEblAction && scenarioType == null) {
      this.scenarioType = previousEblAction.scenarioType;
    }
  }

  protected EblAction(
    String sourcePartyName,
    String targetPartyName,
    BookingAndEblAction previousAction,
    String actionTitle,
    Set<Integer> expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
    if (previousAction instanceof EblAction previousEblAction && scenarioType == null) {
      this.scenarioType = previousEblAction.scenarioType;
    }
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

  protected DynamicScenarioParameters getDSP() {
    return getDspSupplier().get();
  }

  protected String getMarkdownHumanReadablePrompt(
      Map<String, String> replacementsMap, String... fileNames) {

    return Arrays.stream(fileNames)
        .map(
            fileName ->
                IOToolkit.templateFileToText(
                    "/standards/ebl/instructions/" + fileName, replacementsMap))
        .collect(Collectors.joining());
  }

  protected EblAction getPreviousEblAction() {
    return (EblAction) previousAction;
  }

  protected Consumer<JsonNode> getCarrierPayloadConsumer() {
    return getPreviousEblAction().getCarrierPayloadConsumer();
  }

  protected Supplier<JsonNode> getCarrierPayloadSupplier() {
    return getPreviousEblAction().getCarrierPayloadSupplier();
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dspReference::get;
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dspReference::set;
  }

  protected void updateDSPFromSIResponsePayload(ConformanceExchange exchange) {
    DynamicScenarioParameters dsp = dspReference.get();

    JsonNode responseJsonNode = exchange.getResponse().message().body().getJsonBody();
    var newShippingInstructionsReference =
        responseJsonNode.path("shippingInstructionsReference").asText(null);
    var newTransportDocumentReference =
        responseJsonNode.path("transportDocumentReference").asText(null);

    var updatedDsp = dsp;
    updatedDsp =
        updateIfNotNull(
            updatedDsp,
            newShippingInstructionsReference,
            updatedDsp::withShippingInstructionsReference);
    updatedDsp =
        updateIfNotNull(
            updatedDsp, newTransportDocumentReference, updatedDsp::withTransportDocumentReference);

    // SD-1997 gradually wiping out from production orchestrator states the big docs that should not have been added to the DSP
    updatedDsp = updatedDsp.withShippingInstructions(null).withUpdatedShippingInstructions(null);

    if (!dsp.equals(updatedDsp)) {
      dspReference.set(updatedDsp);
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
      UUID notificationExchangeUuid,
      String expectedApiVersion,
      JsonSchemaValidator notificationSchemaValidator,
      ShippingInstructionsStatus expectedStatus,
      JsonContentCheck... extraChecks) {
    return getSINotificationChecks(
        notificationExchangeUuid,
        expectedApiVersion,
        notificationSchemaValidator,
        expectedStatus,
        null,
        extraChecks);
  }

  protected Stream<ActionCheck> getSINotificationChecks(
      UUID notificationExchangeUuid,
      String expectedApiVersion,
      JsonSchemaValidator notificationSchemaValidator,
      ShippingInstructionsStatus expectedStatus,
      ShippingInstructionsStatus expectedUpdatedStatus,
      JsonContentCheck... extraChecks) {
    String titlePrefix = "[Notification]";
    return Stream.of(
        new HttpMethodCheck(titlePrefix, EblRole::isCarrier, notificationExchangeUuid, "POST"),
        new UrlPathCheck(
            titlePrefix,
            EblRole::isCarrier,
            notificationExchangeUuid,
            "/v3/shipping-instructions-notifications"),
        new ResponseStatusCheck(titlePrefix, EblRole::isShipper, notificationExchangeUuid, 204),
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
        new CarrierSiNotificationPayloadRequestConformanceCheck(
            notificationExchangeUuid,
            expectedApiVersion,
            expectedStatus,
            expectedUpdatedStatus,
            scenarioType,
            getDspSupplier(),
            extraChecks));
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
    return Stream.of(
        new HttpMethodCheck(titlePrefix, EblRole::isCarrier, notificationExchangeUuid, "POST"),
        new UrlPathCheck(
            titlePrefix,
            EblRole::isCarrier,
            notificationExchangeUuid,
            "/v3/transport-document-notifications"),
        new ResponseStatusCheck(titlePrefix, EblRole::isShipper, notificationExchangeUuid, 204),
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
        new CarrierTdNotificationPayloadRequestConformanceCheck(
            notificationExchangeUuid,
            expectedApiVersion,
            transportDocumentStatus,
            tdrIsKnown,
            getDspSupplier(),
            scenarioType));
  }
}
