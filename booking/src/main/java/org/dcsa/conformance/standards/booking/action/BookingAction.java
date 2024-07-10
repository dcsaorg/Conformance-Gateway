package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingNotificationDataPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standards.booking.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

public abstract class BookingAction extends ConformanceAction {
  protected final int expectedStatus;
  private final OverwritingReference<DynamicScenarioParameters> dspReference;

  public BookingAction(
      String sourcePartyName,
      String targetPartyName,
      BookingAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
    this.dspReference =
        previousAction == null
            ? new OverwritingReference<>(null, new DynamicScenarioParameters(ScenarioType.REGULAR, null, null, null, null,null))
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

  protected DynamicScenarioParameters updateDSPFromBookingAction(ConformanceExchange exchange, DynamicScenarioParameters dynamicScenarioParameters) {
    return dynamicScenarioParameters;
  }


  protected BookingAction getPreviousBookingAction() {
    return (BookingAction) previousAction;
  }

  protected Consumer<CarrierScenarioParameters> getCspConsumer() {
    return getPreviousBookingAction().getCspConsumer();
  }

  protected Supplier<CarrierScenarioParameters> getCspSupplier() {
    return getPreviousBookingAction().getCspSupplier();
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dspReference::get;
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dspReference::set;
  }

  private <T> DynamicScenarioParameters updateIfNotNull(DynamicScenarioParameters dsp, T value, Function<T, DynamicScenarioParameters> with) {
    if (value == null) {
      return dsp;
    }
    return with.apply(value);
  }

  private static BookingState parseBookingState(String v) {
    if (v == null) {
      return null;
    }
    try {
      return BookingState.fromWireName(v);
    } catch (IllegalArgumentException e) {
      // Do not assume conformant payload.
      return null;
    }
  }

  protected void updateDSPFromResponsePayload(ConformanceExchange exchange) {
    DynamicScenarioParameters dsp = dspReference.get();

    JsonNode responseJsonNode = exchange.getResponse().message().body().getJsonBody();
    JsonNode requestJsonNode = exchange.getRequest().message().body().getJsonBody();
    String newCbr = getCbrFromNotificationPayload(requestJsonNode) != null ?
      getCbrFromNotificationPayload(requestJsonNode) :
      responseJsonNode.path("carrierBookingReference").asText(null);
    var newCbrr = responseJsonNode.path("carrierBookingRequestReference").asText(null);
    var newBookingStatus = parseBookingState(responseJsonNode.path("bookingStatus").asText(null));
    var newAmendedBookingStatus = parseBookingState(responseJsonNode.path("amendedBookingStatus").asText(null));

    var updatedDsp = dsp;
    updatedDsp = updateIfNotNull(updatedDsp, newCbrr, updatedDsp::withCarrierBookingRequestReference);
    updatedDsp = updateIfNotNull(updatedDsp, newCbr, updatedDsp::withCarrierBookingReference);
    updatedDsp = updateIfNotNull(updatedDsp, newBookingStatus, updatedDsp::withBookingStatus);
    updatedDsp = updateIfNotNull(updatedDsp, newAmendedBookingStatus, updatedDsp::withAmendedBookingStatus);

    updatedDsp = updateDSPFromBookingAction(exchange, updatedDsp);

    if (!dsp.equals(updatedDsp)) {
      dspReference.set(updatedDsp);
    }
  }

  private String getCbrFromNotificationPayload(JsonNode requestJsonNode) {
    return requestJsonNode.path("data").path("carrierBookingReference").asText(null);
  }
  protected Stream<ActionCheck> getNotificationChecks(
      String expectedApiVersion,
      JsonSchemaValidator notificationSchemaValidator,
      BookingState bookingState,
      BookingState amendedBookingState) {
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
                getMatchedNotificationExchangeUuid(), bookingState, amendedBookingState),
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
                    bookingState.wireName()),
            amendedBookingState == null
                ? null
                : new JsonAttributeCheck(
                    titlePrefix,
                    BookingRole::isCarrier,
                    getMatchedNotificationExchangeUuid(),
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/data/amendedBookingStatus"),
                    amendedBookingState.wireName()))
        .filter(Objects::nonNull);
  }
}
