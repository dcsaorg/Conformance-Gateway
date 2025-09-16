package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingNotificationDataPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.*;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

public abstract class BookingAction extends BookingAndEblAction {

  protected final int expectedStatus;

  protected BookingAction(
      String sourcePartyName,
      String targetPartyName,
      BookingAndEblAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
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

  protected BookingAction getPreviousBookingAction() {
    return (BookingAction) previousAction;
  }

  protected Consumer<JsonNode> getBookingPayloadConsumer() {
    return getPreviousBookingAction().getBookingPayloadConsumer();
  }

  protected Supplier<JsonNode> getBookingPayloadSupplier() {
    return getPreviousBookingAction().getBookingPayloadSupplier();
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dspReference::get;
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dspReference::set;
  }

  private <T> DynamicScenarioParameters updateIfNotNull(
      DynamicScenarioParameters dsp, T value, Function<T, DynamicScenarioParameters> with) {
    if (value == null) {
      return dsp;
    }
    return with.apply(value);
  }

  protected void updateDSPFromResponsePayload(ConformanceExchange exchange) {
    JsonNode responseJsonNode = exchange.getResponse().message().body().getJsonBody();
    JsonNode requestJsonNode = exchange.getRequest().message().body().getJsonBody();
    String newCbr =
        getCbrFromNotificationPayload(requestJsonNode) != null
            ? getCbrFromNotificationPayload(requestJsonNode)
            : responseJsonNode.path("carrierBookingReference").asText(null);
    var newCbrr = responseJsonNode.path("carrierBookingRequestReference").asText(null);

    DynamicScenarioParameters dsp = dspReference.get();
    var updatedDsp = dsp;
    updatedDsp =
        updateIfNotNull(updatedDsp, newCbrr, updatedDsp::withCarrierBookingRequestReference);
    updatedDsp = updateIfNotNull(updatedDsp, newCbr, updatedDsp::withCarrierBookingReference);
    // SD-1997 gradually wiping out from production orchestrator states the big docs that should not
    // have been added to the DSP
    updatedDsp = updatedDsp.withBooking(null).withUpdatedBooking(null);

    if (!dsp.equals(updatedDsp)) {
      dspReference.set(updatedDsp);
    }
  }

  private String getCbrFromNotificationPayload(JsonNode requestJsonNode) {
    return requestJsonNode.path("data").path("carrierBookingReference").asText(null);
  }

  protected String getMarkdownHumanReadablePrompt(String... fileNames) {
    Map<String, String> replacementsMap =
        Map.ofEntries(
            Map.entry(
                "WITH_CBR_OR_CBRR_PLACEHOLDER",
                withCbrOrCbrr(
                    getDspSupplier().get().carrierBookingReference(),
                    getDspSupplier().get().carrierBookingRequestReference())));
    return Arrays.stream(fileNames)
        .map(
            fileName ->
                IOToolkit.templateFileToText(
                    "/standards/booking/instructions/" + fileName, replacementsMap))
        .collect(Collectors.joining());
  }

  protected String getMarkdownHumanReadablePrompt(ScenarioType scenarioType, String... fileNames) {
    Map<String, String> replacementsMap =
        Map.ofEntries(
            Map.entry(
                "WITH_CBR_OR_CBRR_PLACEHOLDER",
                withCbrOrCbrr(
                    getDspSupplier().get().carrierBookingReference(),
                    getDspSupplier().get().carrierBookingRequestReference())));
    return Arrays.stream(fileNames)
        .map(
            fileName ->
                IOToolkit.templateFileToText(
                    "/standards/booking/instructions/" + fileName, replacementsMap))
        .collect(Collectors.joining())
        .replace("SCENARIO_TYPE", scenarioType.name());
  }

  protected static String withCbrOrCbrr(String cbr, String cbrr) {
    return (cbr != null ? "with CBR '%s'".formatted(cbr) : "")
        + (cbr != null && cbrr != null ? " and " : "")
        + (cbrr != null ? "with CBRR '%s'".formatted(cbrr) : "");
  }

  public static String createMessageForUIPrompt(String message, String cbr, String cbrr) {
    return message + " " + withCbrOrCbrr(cbr, cbrr);
  }

  protected Stream<ActionCheck> getNotificationChecks(
      String expectedApiVersion,
      JsonSchemaValidator notificationSchemaValidator,
      BookingState bookingState,
      BookingState amendedBookingState) {
    return getNotificationChecks(
        expectedApiVersion, notificationSchemaValidator, bookingState, amendedBookingState, null);
  }

  protected Stream<ActionCheck> getNotificationChecks(
      String expectedApiVersion,
      JsonSchemaValidator notificationSchemaValidator,
      BookingState bookingState,
      BookingState amendedBookingState,
      BookingCancellationState bookingCancellationState) {
    String titlePrefix = "[Notification]";
    var cbr = dspReference.get().carrierBookingReference();
    var cbrr = dspReference.get().carrierBookingRequestReference();
    return Stream.of(
            new HttpMethodCheck(
                titlePrefix, BookingRole::isCarrier, getMatchedNotificationExchangeUuid(), "POST"),
            new UrlPathCheck(
                titlePrefix,
                BookingRole::isCarrier,
                getMatchedNotificationExchangeUuid(),
                "/v2/booking-notifications"),
            new ResponseStatusCheck(
                titlePrefix, BookingRole::isShipper, getMatchedNotificationExchangeUuid(), 204),
            new CarrierBookingNotificationDataPayloadRequestConformanceCheck(
                getMatchedNotificationExchangeUuid(),
                bookingState,
                amendedBookingState,
                bookingCancellationState,
                getDspSupplier()),
            ApiHeaderCheck.createNotificationCheck(
                titlePrefix,
                BookingRole::isCarrier,
                getMatchedNotificationExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            ApiHeaderCheck.createNotificationCheck(
                titlePrefix,
                BookingRole::isShipper,
                getMatchedNotificationExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                titlePrefix,
                BookingRole::isCarrier,
                getMatchedNotificationExchangeUuid(),
                HttpMessageType.REQUEST,
                notificationSchemaValidator),
            cbr == null
                ? null
                : new JsonAttributeCheck(
                    titlePrefix,
                    BookingRole::isCarrier,
                    getMatchedNotificationExchangeUuid(),
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/data/carrierBookingReference"),
                    cbr),
            cbrr == null
                ? null
                : new JsonAttributeCheck(
                    titlePrefix,
                    BookingRole::isCarrier,
                    getMatchedNotificationExchangeUuid(),
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/data/carrierBookingRequestReference"),
                    cbrr),
            bookingState == null
                ? null
                : new JsonAttributeCheck(
                    titlePrefix,
                    BookingRole::isCarrier,
                    getMatchedNotificationExchangeUuid(),
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/data/bookingStatus"),
                    bookingState.name()),
            amendedBookingState == null
                ? null
                : new JsonAttributeCheck(
                    titlePrefix,
                    BookingRole::isCarrier,
                    getMatchedNotificationExchangeUuid(),
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/data/amendedBookingStatus"),
                    amendedBookingState.name()),
            bookingCancellationState == null
                ? null
                : new JsonAttributeCheck(
                    titlePrefix,
                    BookingRole::isCarrier,
                    getMatchedNotificationExchangeUuid(),
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/data/bookingCancellationStatus"),
                    bookingCancellationState.name()))
        .filter(Objects::nonNull);
  }
}
