package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;

public class Shipper_GetAmendedBooking404Action extends BookingAction {

  public Shipper_GetAmendedBooking404Action(
      String carrierPartyName, String shipperPartyName, BookingAction previousAction) {
    super(
        shipperPartyName,
        carrierPartyName,
        previousAction,
        "GET (amended content, non-existing)",
        404);
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
        .put("cbrr", getDspSupplier().get().carrierBookingRequestReference())
        .put("amendedContent", true);
  }

  @Override
  public String getHumanReadablePrompt() {
    return createMessageForUIPrompt(
        "GET the (non-existing) amendment to the booking",
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
            new QueryParamCheck(
                BookingRole::isShipper, getMatchedExchangeUuid(), "amendedContent", "true"),
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
                expectedApiVersion))
        // .filter(Objects::nonNull)
        ;
      }
    };
  }
}
