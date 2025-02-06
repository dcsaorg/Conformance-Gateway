package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingNotificationDataPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.*;

public abstract class BookingAction extends ConformanceAction {
  protected final int expectedStatus;
  private final OverwritingReference<DynamicScenarioParameters> dspReference;

  protected BookingAction(
      String sourcePartyName,
      String targetPartyName,
      BookingAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
    this.dspReference =
        previousAction == null
            ? new OverwritingReference<>(null, new DynamicScenarioParameters(ScenarioType.REGULAR, null, null, null, null))
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

  protected void updateDSPFromResponsePayload(ConformanceExchange exchange) {
    JsonNode responseJsonNode = exchange.getResponse().message().body().getJsonBody();
    JsonNode requestJsonNode = exchange.getRequest().message().body().getJsonBody();
    String newCbr = getCbrFromNotificationPayload(requestJsonNode) != null ?
      getCbrFromNotificationPayload(requestJsonNode) :
      responseJsonNode.path("carrierBookingReference").asText(null);
    var newCbrr = responseJsonNode.path("carrierBookingRequestReference").asText(null);

    DynamicScenarioParameters dsp = dspReference.get();
    var updatedDsp = dsp;
    updatedDsp = updateIfNotNull(updatedDsp, newCbrr, updatedDsp::withCarrierBookingRequestReference);
    updatedDsp = updateIfNotNull(updatedDsp, newCbr, updatedDsp::withCarrierBookingReference);
    // SD-1997 gradually wiping out from production orchestrator states the big docs that should not have been added to the DSP
    updatedDsp = updatedDsp.withBooking(null).withUpdatedBooking(null);

    if (!dsp.equals(updatedDsp)) {
      dspReference.set(updatedDsp);
    }
  }

  private String getCbrFromNotificationPayload(JsonNode requestJsonNode) {
    return requestJsonNode.path("data").path("carrierBookingReference").asText(null);
  }

  protected String createHumanReadablePromptMessage(String message, String cbr, String cbrr) {
    return message
        + (cbr != null ? " with CBR '%s'".formatted(cbr) : "")
        + (cbr != null && cbrr != null ? " and" : "")
        + (cbrr != null ? " with CBRR '%s'".formatted(cbrr) : "");
  }

  protected Stream<ActionCheck> getNotificationChecks(
    String expectedApiVersion,
    JsonSchemaValidator notificationSchemaValidator,
    BookingState bookingState,
    BookingState amendedBookingState) {
    return getNotificationChecks(expectedApiVersion, notificationSchemaValidator, bookingState, amendedBookingState,null);
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
                getMatchedNotificationExchangeUuid(), bookingState, amendedBookingState, bookingCancellationState),
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
