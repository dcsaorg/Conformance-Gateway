package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingRefStatusPayloadResponseConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

import java.util.Objects;
import java.util.stream.Stream;

@Getter
@Slf4j
public class UC13_Shipper_CancelConfirmedBookingAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC13_Shipper_CancelConfirmedBookingAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC13", 200);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC11: Cancel a confirmed booking");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return getCspSupplier().get().toJson();
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("cbr", getDspSupplier().get().carrierBookingReference());
    return jsonNode;
  }

  @Override
  protected DynamicScenarioParameters updateDSPFromBookingAction(ConformanceExchange exchange, DynamicScenarioParameters dynamicScenarioParameters) {
    var body = exchange.getResponse().message().body().getJsonBody();
    return dynamicScenarioParameters.withUpdatedBooking(body);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        String cbr = dsp.carrierBookingReference();
        var expectedBookingStatus = getDspSupplier().get().bookingStatus();
        var expectedAmendedBookingStatus = getDspSupplier().get().amendedBookingStatus();
        var expectedBookingCancellationStatus = getDspSupplier().get().bookingCancellationStatus();
        Stream<ActionCheck> primaryExchangeChecks = Stream.of(
          new HttpMethodCheck(BookingRole::isShipper, getMatchedExchangeUuid(), "PATCH"),
          new UrlPathCheck(BookingRole::isShipper, getMatchedExchangeUuid(), "/v2/bookings/%s".formatted(cbr)),
          new ResponseStatusCheck(
            BookingRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
          new ApiHeaderCheck(
            BookingRole::isShipper,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new ApiHeaderCheck(
            BookingRole::isCarrier,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new JsonSchemaCheck(
            BookingRole::isShipper,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            requestSchemaValidator));
        return Stream.concat(
          Stream.concat(primaryExchangeChecks,
            Stream.of(
              expectedBookingStatus == null ? null: new CarrierBookingRefStatusPayloadResponseConformanceCheck(
                getMatchedExchangeUuid(),
                expectedBookingStatus,
                expectedAmendedBookingStatus,
                expectedBookingCancellationStatus
              ),
              new JsonSchemaCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator)).filter(Objects::nonNull)),
          expectedBookingStatus != null ?  getNotificationChecks(
            expectedApiVersion,
            notificationSchemaValidator,
            expectedBookingStatus,
            expectedAmendedBookingStatus,
            expectedBookingCancellationStatus): Stream.empty());
      }
    };
  }
}
