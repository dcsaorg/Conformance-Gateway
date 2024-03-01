package org.dcsa.conformance.standards.booking.action;


import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.BookingChecks;
import org.dcsa.conformance.standards.booking.checks.CarrierGetBookingPayloadResponseConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

public class Shipper_GetBookingAction extends BookingAction {

  private final BookingState expectedBookingStatus;
  private final BookingState expectedAmendedBookingStatus;
  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean requestAmendedContent;

  public Shipper_GetBookingAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      BookingState expectedBookingStatus,
      BookingState expectedAmendedBookingStatus,
      JsonSchemaValidator responseSchemaValidator,
      boolean requestAmendedStatus) {
    super(
        shipperPartyName,
        carrierPartyName,
        previousAction,
        requestAmendedStatus ? "GET (amended content)" : "GET",
        200);
    this.expectedBookingStatus = expectedBookingStatus;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
    this.responseSchemaValidator = responseSchemaValidator;
    this.requestAmendedContent = requestAmendedStatus;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
        .put("cbrr", getDspSupplier().get().carrierBookingRequestReference())
        .put("amendedContent", requestAmendedContent);
  }

  @Override
  public String getHumanReadablePrompt() {
    return "GET the booking with CBR '%s' and CBRR '%s'"
        .formatted(
            getDspSupplier().get().carrierBookingReference(),
            getDspSupplier().get().carrierBookingRequestReference());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(
                BookingRole::isShipper,
                getMatchedExchangeUuid(),
                "/v2/bookings/" + getDspSupplier().get().carrierBookingRequestReference()),
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
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator),
            new CarrierGetBookingPayloadResponseConformanceCheck(
                getMatchedExchangeUuid(),
                expectedBookingStatus,
                expectedAmendedBookingStatus,
                requestAmendedContent),
            BookingChecks.responseContentChecks(getMatchedExchangeUuid(), getCspSupplier(), getDspSupplier(), expectedBookingStatus, expectedAmendedBookingStatus))
        ;
      }
    };
  }
}
