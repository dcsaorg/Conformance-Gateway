package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingNotificationDataPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.stream.Stream;

@Getter
public class UC14_Carrier_ProcessBookingCancellationAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean isCancellationConfirmed;

  public UC14_Carrier_ProcessBookingCancellationAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      boolean isCancellationConfirmed) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC14", 204);
    this.requestSchemaValidator = requestSchemaValidator;
    this.isCancellationConfirmed = isCancellationConfirmed;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC12: Complete the booking request with CBR '%s'"
        .formatted(
            getDspSupplier().get().carrierBookingReference()));
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    var dsp = getDspSupplier().get();
    return jsonNode
        .put("cbrr", dsp.carrierBookingRequestReference())
        .put("cbr", dsp.carrierBookingReference())
        .put("isCancellationConfirmed", isCancellationConfirmed);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        var bookingStatus = dsp.bookingStatus();
        var amendedBookingStatus = dsp.amendedBookingStatus();
        return Stream.of(
            new UrlPathCheck(
                BookingRole::isCarrier, getMatchedExchangeUuid(), "/v2/booking-notifications"),
            new ResponseStatusCheck(
                BookingRole::isShipper, getMatchedExchangeUuid(), expectedStatus),
            new CarrierBookingNotificationDataPayloadRequestConformanceCheck(
                getMatchedExchangeUuid(),
                isCancellationConfirmed ? BookingState.CANCELLED : bookingStatus,
                isCancellationConfirmed
                  ? BookingState.AMENDMENT_CANCELLED
                  : amendedBookingStatus),
            ApiHeaderCheck.createNotificationCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            ApiHeaderCheck.createNotificationCheck(
                BookingRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator));
      }
    };
  }
}
