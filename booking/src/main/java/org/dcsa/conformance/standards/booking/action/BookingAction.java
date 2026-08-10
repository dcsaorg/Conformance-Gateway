package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingNotificationDataPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.booking.checks.CarrierStatusScenario;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standardscommons.action.BookingAndEblAction;
import org.dcsa.conformance.standardscommons.party.BookingDynamicScenarioParameters;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public abstract class BookingAction extends BookingAndEblAction {

  protected final int expectedStatus;
  private final boolean isWithNotifications;

  protected BookingAction(
    String sourcePartyName,
    String targetPartyName,
    BookingAndEblAction previousAction,
    String actionTitle,
    int expectedStatus,
    boolean isWithNotifications) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
    this.isWithNotifications = isWithNotifications;
  }

  @Override
  public void reset() {
    super.reset();
    if (previousAction != null) {
      getBookingDspReference().set(null);
    }
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (getBookingDspReference().hasCurrentValue()) {
      jsonState.set("currentDsp", getBookingDspReference().get().toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode dspNode = jsonState.get("currentDsp");
    if (dspNode != null) {
      getBookingDspReference().set(BookingDynamicScenarioParameters.fromJson(dspNode));
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

  protected Supplier<BookingDynamicScenarioParameters> getDspSupplier() {
    return getBookingDspReference()::get;
  }

  protected Consumer<BookingDynamicScenarioParameters> getDspConsumer() {
    return getBookingDspReference()::set;
  }

  private <T> BookingDynamicScenarioParameters updateIfNotNull(
    BookingDynamicScenarioParameters dsp,
    T value,
    Function<T, BookingDynamicScenarioParameters> with) {
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

    BookingDynamicScenarioParameters dsp = getBookingDspReference().get();
    var updatedDsp = dsp;
    updatedDsp =
      updateIfNotNull(updatedDsp, newCbrr, updatedDsp::withCarrierBookingRequestReference);
    updatedDsp = updateIfNotNull(updatedDsp, newCbr, updatedDsp::withCarrierBookingReference);

    if (!dsp.equals(updatedDsp)) {
      getBookingDspReference().set(updatedDsp);
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
      .replace(
        "SCENARIO_TYPE_INSTRUCTION",
        scenarioType == ScenarioType.ANY
          ? "You may use any supported Booking cargo type."
          : "Make sure the booking type remains %s.".formatted(scenarioType.displayName()));
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
    return Stream.of(
      new HttpMethodCheck(titlePrefix, BookingRole::isCarrier, getMatchedNotificationExchangeUuid(), "POST"),
      new UrlPathCheck(titlePrefix, BookingRole::isCarrier, getMatchedNotificationExchangeUuid(), "/v2/booking-notifications"),
      new ResponseStatusCheck(titlePrefix, BookingRole::isShipper, getMatchedNotificationExchangeUuid(), 204)
        .withRelevance(isWithNotifications),
      new JsonSchemaCheck(titlePrefix, BookingRole::isCarrier, getMatchedNotificationExchangeUuid(), HttpMessageType.REQUEST, notificationSchemaValidator),
      ApiHeaderCheck.createNotificationCheck(titlePrefix, BookingRole::isCarrier, getMatchedNotificationExchangeUuid(), HttpMessageType.REQUEST, expectedApiVersion),
      ApiHeaderCheck.createNotificationCheck(titlePrefix, BookingRole::isShipper, getMatchedNotificationExchangeUuid(), HttpMessageType.RESPONSE, expectedApiVersion)
        .withRelevance(isWithNotifications),
      new CarrierBookingNotificationDataPayloadRequestConformanceCheck(
        getMatchedNotificationExchangeUuid(),
        bookingState,
        amendedBookingState,
        bookingCancellationState,
        getDspSupplier()));
  }

  protected Stream<ActionCheck> getSimpleNotificationChecks(
    String expectedApiVersion,
    JsonSchemaValidator requestSchemaValidator,
    BookingState bookingState) {
    return getSimpleNotificationChecks(
      expectedApiVersion, requestSchemaValidator, bookingState, null, null);
  }

  protected Stream<ActionCheck> getSimpleNotificationChecks(
    String expectedApiVersion,
    JsonSchemaValidator requestSchemaValidator,
    BookingState bookingState,
    BookingState amendedBookingState) {
    return getSimpleNotificationChecks(
      expectedApiVersion, requestSchemaValidator, bookingState, amendedBookingState, null);
  }

  protected Stream<ActionCheck> getSimpleNotificationChecks(
    String expectedApiVersion,
    JsonSchemaValidator requestSchemaValidator,
    BookingState bookingState,
    BookingState amendedBookingState,
    BookingCancellationState cancellationState) {
    String titlePrefix = "[Notification]";
    return Stream.of(
      new HttpMethodCheck(titlePrefix, BookingRole::isCarrier, getMatchedExchangeUuid(), "POST"),
      new UrlPathCheck(titlePrefix, BookingRole::isCarrier, getMatchedExchangeUuid(), "/v2/booking-notifications"),
      new ResponseStatusCheck(titlePrefix, BookingRole::isShipper, getMatchedExchangeUuid(), expectedStatus)
        .withRelevance(isWithNotifications()),
      new JsonSchemaCheck(
        titlePrefix,
        BookingRole::isCarrier,
        getMatchedExchangeUuid(),
        HttpMessageType.REQUEST,
        requestSchemaValidator),
      ApiHeaderCheck.createNotificationCheck(
        titlePrefix,
        BookingRole::isCarrier,
        getMatchedExchangeUuid(),
        HttpMessageType.REQUEST,
        expectedApiVersion),
      ApiHeaderCheck.createNotificationCheck(
          titlePrefix,
          BookingRole::isShipper,
          getMatchedExchangeUuid(),
          HttpMessageType.RESPONSE,
          expectedApiVersion)
        .withRelevance(isWithNotifications()),
      new CarrierBookingNotificationDataPayloadRequestConformanceCheck(
        getMatchedExchangeUuid(),
        bookingState,
        amendedBookingState,
        cancellationState,
        getDspSupplier()));
  }

  protected Stream<ActionCheck> getSimpleNotificationChecks(
    String expectedApiVersion,
    JsonSchemaValidator requestSchemaValidator,
    CarrierStatusScenario carrierStatusScenario) {
    String titlePrefix = "[Notification]";
    return Stream.of(
      new HttpMethodCheck(titlePrefix, BookingRole::isCarrier, getMatchedExchangeUuid(), "POST"),
      new UrlPathCheck(titlePrefix, BookingRole::isCarrier, getMatchedExchangeUuid(), "/v2/booking-notifications"),
      new ResponseStatusCheck(titlePrefix, BookingRole::isShipper, getMatchedExchangeUuid(), expectedStatus)
        .withRelevance(isWithNotifications()),
      new JsonSchemaCheck(
        titlePrefix,
        BookingRole::isCarrier,
        getMatchedExchangeUuid(),
        HttpMessageType.REQUEST,
        requestSchemaValidator),
      ApiHeaderCheck.createNotificationCheck(
        titlePrefix,
        BookingRole::isCarrier,
        getMatchedExchangeUuid(),
        HttpMessageType.REQUEST,
        expectedApiVersion),
      ApiHeaderCheck.createNotificationCheck(
          titlePrefix,
          BookingRole::isShipper,
          getMatchedExchangeUuid(),
          HttpMessageType.RESPONSE,
          expectedApiVersion)
        .withRelevance(isWithNotifications()),
      new CarrierBookingNotificationDataPayloadRequestConformanceCheck(getMatchedExchangeUuid(), carrierStatusScenario, getDspSupplier()));
  }
}
