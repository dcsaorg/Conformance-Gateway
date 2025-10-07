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

import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standardscommons.action.BookingAndEblAction;
import org.dcsa.conformance.standardscommons.party.EblDynamicScenarioParameters;
import org.dcsa.conformance.standards.ebl.checks.CarrierSiNotificationPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.ebl.checks.CarrierTdNotificationPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.ebl.party.*;

@Getter
public abstract class EblAction extends BookingAndEblAction {

  protected final Set<Integer> expectedStatus;
  private final boolean isWithNotifications;

  protected EblAction(
      String sourcePartyName,
      String targetPartyName,
      BookingAndEblAction previousAction,
      String actionTitle,
      int expectedStatus,
      boolean isWithNotifications) {
    this(sourcePartyName, targetPartyName, previousAction, actionTitle, Set.of(expectedStatus), isWithNotifications);
  }

  protected EblAction(
    String sourcePartyName,
    String targetPartyName,
    BookingAndEblAction previousAction,
    String actionTitle,
    Set<Integer> expectedStatus,
    boolean isWithNotifications) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
    this.isWithNotifications = isWithNotifications;
  }

  @Override
  public void reset() {
    super.reset();
    if (previousAction != null) {
      getEblDspReference().set(null);
    }
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (getEblDspReference().hasCurrentValue()) {
      jsonState.set("currentDsp", getEblDspReference().get().toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode dspNode = jsonState.get("currentDsp");
    if (dspNode != null) {
      getEblDspReference().set(EblDynamicScenarioParameters.fromJson(dspNode));
    }
  }

  protected EblDynamicScenarioParameters getDSP() {
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

  protected Supplier<EblDynamicScenarioParameters> getDspSupplier() {
    return getEblDspReference()::get;
  }

  protected Consumer<EblDynamicScenarioParameters> getDspConsumer() {
    return getEblDspReference()::set;
  }

  protected void updateDSPFromSIResponsePayload(ConformanceExchange exchange) {
    EblDynamicScenarioParameters dsp = getEblDspReference().get();

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
      getEblDspReference().set(updatedDsp);
    }
  }

  private <T> EblDynamicScenarioParameters updateIfNotNull(
          EblDynamicScenarioParameters dsp, T value, Function<T, EblDynamicScenarioParameters> with) {
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
        new ResponseStatusCheck(titlePrefix, EblRole::isShipper, notificationExchangeUuid, 204)
            .setApplicable(isWithNotifications),
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
                expectedApiVersion)
            .setApplicable(isWithNotifications),
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
        new ResponseStatusCheck(titlePrefix, EblRole::isShipper, notificationExchangeUuid, 204)
            .setApplicable(isWithNotifications),
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
                expectedApiVersion)
            .setApplicable(isWithNotifications),
        new JsonSchemaCheck(
            titlePrefix,
            EblRole::isCarrier,
            notificationExchangeUuid,
            HttpMessageType.REQUEST,
            notificationSchemaValidator),
        new CarrierTdNotificationPayloadRequestConformanceCheck(
            notificationExchangeUuid,
            transportDocumentStatus,
            tdrIsKnown,
            getDspSupplier()));
  }
}
