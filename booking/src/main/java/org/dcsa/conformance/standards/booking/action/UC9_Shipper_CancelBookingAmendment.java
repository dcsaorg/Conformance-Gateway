package org.dcsa.conformance.standards.booking.action;

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
public class UC9_Shipper_CancelBookingAmendment extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC9_Shipper_CancelBookingAmendment(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator responseSchemaValidator,
    JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC9", 200);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC9: Cancel Amendment to confirmed booking");
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("cbrr", getDspSupplier().get().carrierBookingRequestReference());
    jsonNode.put("cbr", getDspSupplier().get().carrierBookingReference());
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        String reference = dsp.carrierBookingReference() !=  null ? dsp.carrierBookingReference() : dsp.carrierBookingRequestReference();
        var expectedBookingStatus = getDspSupplier().get().bookingStatus();
        return Stream.concat(
          Stream.concat(createPrimarySubChecks("PATCH", expectedApiVersion, "/v2/bookings/%s".formatted(reference)),
            Stream.of(new CarrierBookingRefStatusPayloadResponseConformanceCheck(
              getMatchedExchangeUuid(),
              expectedBookingStatus,
              BookingState.AMENDMENT_CANCELLED),
              new JsonSchemaCheck(
                BookingRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator),
              new JsonSchemaCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator))),
          getNotificationChecks(
            expectedApiVersion,
            notificationSchemaValidator,
            expectedBookingStatus,
            BookingState.AMENDMENT_CANCELLED));
      }
    };
  }
}
