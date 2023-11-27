package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingNotificationDataPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standards.booking.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class BookingAction extends ConformanceAction {
  protected final int expectedStatus;

  public BookingAction(
      String sourcePartyName,
      String targetPartyName,
      BookingAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
  }

  protected BookingAction getPreviousBookingAction() {
    return (BookingAction) previousAction;
  }

  protected Consumer<CarrierScenarioParameters> getCspConsumer() {
    return getPreviousBookingAction().getCspConsumer();
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return getPreviousBookingAction().getDspConsumer();
  }

  protected Supplier<CarrierScenarioParameters> getCspSupplier() {
    return getPreviousBookingAction().getCspSupplier();
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return getPreviousBookingAction().getDspSupplier();
  }

  protected void storeCbrAndCbrrIfPresent(ConformanceExchange exchange) {
    JsonNode responseJsonNode = exchange.getResponse().message().body().getJsonBody();
    if (getDspSupplier().get().carrierBookingRequestReference() == null) {
      if (responseJsonNode.has("carrierBookingRequestReference")) {
        getDspConsumer()
            .accept(
                new DynamicScenarioParameters(
                    responseJsonNode.get("carrierBookingRequestReference").asText(),
                    getDspSupplier().get().carrierBookingReference()));
      }
    }
    if (getDspSupplier().get().carrierBookingReference() == null) {
      if (responseJsonNode.has("carrierBookingReference")) {
        getDspConsumer()
            .accept(
                new DynamicScenarioParameters(
                    getDspSupplier().get().carrierBookingRequestReference(),
                    responseJsonNode.get("carrierBookingReference").asText()));
      }
    }
  }

  protected Stream<ActionCheck> getNotificationChecks(
      String expectedApiVersion,
      JsonSchemaValidator notificationSchemaValidator,
      BookingState bookingState,
      BookingState amendedBookingState) {
    String titlePrefix = "[Notification]";
    var cbr = getDspSupplier().get().carrierBookingReference();
    var cbrr = getDspSupplier().get().carrierBookingRequestReference();
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
              amendedBookingState
            ),
            new ApiHeaderCheck(
                titlePrefix,
                BookingRole::isCarrier,
                getMatchedNotificationExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
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
                    List.of("data", "carrierBookingReference"),
                    cbr),
            cbrr == null
                ? null
                : new JsonAttributeCheck(
                    titlePrefix,
                    BookingRole::isCarrier,
                    getMatchedNotificationExchangeUuid(),
                    HttpMessageType.REQUEST,
                    List.of("data", "carrierBookingRequestReference"),
                    cbrr),
            bookingState == null
                ? null
                : new JsonAttributeCheck(
                    titlePrefix,
                    BookingRole::isCarrier,
                    getMatchedNotificationExchangeUuid(),
                    HttpMessageType.REQUEST,
                    List.of("data", "bookingStatus"),
                    bookingState.wireName()),
            amendedBookingState == null
                ? null
                : new JsonAttributeCheck(
                    titlePrefix,
                    BookingRole::isCarrier,
                    getMatchedNotificationExchangeUuid(),
                    HttpMessageType.REQUEST,
                    List.of("data", "amendedBookingStatus"),
                    amendedBookingState.wireName()))
        .filter(Objects::nonNull);
  }
}
