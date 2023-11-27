package org.dcsa.conformance.standards.booking.checks;

import java.util.*;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingState;

public class CarrierBookingNotificationDataPayloadRequestConformanceCheck extends AbstractCarrierPayloadConformanceCheck {

  public CarrierBookingNotificationDataPayloadRequestConformanceCheck(UUID matchedExchangeUuid, BookingState bookingStatus) {
    super(matchedExchangeUuid, HttpMessageType.REQUEST, bookingStatus);
  }

  public CarrierBookingNotificationDataPayloadRequestConformanceCheck(
    UUID matchedExchangeUuid,
    BookingState bookingStatus,
    BookingState expectedAmendedBookingStatus
  ) {
    super(
      matchedExchangeUuid,
      HttpMessageType.REQUEST,
      bookingStatus,
      expectedAmendedBookingStatus,
      false
    );
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.of(
      createSubCheck(
        "[Notification] Validate 'data.bookingStatus' is correct",
        at("/data", this::ensureBookingStatusIsCorrect)
      ),
      createSubCheck(
        "[Notification] Validate 'data.amendedBookingStatus' is correct",
        at("/data", this::ensureAmendedBookingStatusIsCorrect)
      ),
      createSubCheck(
        "[Notification] Validate 'data.carrierBookingReference' is conditionally present",
        at("/data", this::ensureCarrierBookingReferenceCompliance)
      ),
      createSubCheck(
        "[Notification] Validate 'data.reason' is only present on states where it is allowed",
        at("/data", requiredOrExcludedByState(REASON_STATES, "reason"))
      )
    );
  }
}
