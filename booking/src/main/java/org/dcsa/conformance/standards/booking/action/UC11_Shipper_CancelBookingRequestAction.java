package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingRefStatusPayloadResponseConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.stream.Stream;

@Getter
@Slf4j
public class UC11_Shipper_CancelBookingRequestAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC11_Shipper_CancelBookingRequestAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC11", 200);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC11: Cancel an entire booking");
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
    jsonNode.put("cbrr", getDspSupplier().get().carrierBookingRequestReference());
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        String cbrr = dsp.carrierBookingRequestReference();
        Stream<ActionCheck> primaryExchangeChecks = Stream.of(
            new HttpMethodCheck(BookingRole::isShipper, getMatchedExchangeUuid(), "PATCH"),
            new UrlPathCheck(BookingRole::isShipper, getMatchedExchangeUuid(), "/v2/bookings/%s".formatted(cbrr)),
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
            Stream.of(new CarrierBookingRefStatusPayloadResponseConformanceCheck(
              getMatchedExchangeUuid(),
              BookingState.CANCELLED
            ),
              new JsonSchemaCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator))),
          getNotificationChecks(
            expectedApiVersion,
            notificationSchemaValidator,
            BookingState.CANCELLED,
            null));
      }
    };
  }
}
