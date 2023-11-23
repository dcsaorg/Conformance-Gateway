package org.dcsa.conformance.standards.booking.checks;

import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.*;
import java.util.stream.Stream;

public class CarrierBookingRefStatusPayloadResponseConformanceCheck extends AbstractCarrierPayloadResponseConformanceCheck {

  public CarrierBookingRefStatusPayloadResponseConformanceCheck(UUID matchedExchangeUuid, BookingState bookingStatus) {
    this(matchedExchangeUuid, bookingStatus, null, false);
  }

  public CarrierBookingRefStatusPayloadResponseConformanceCheck(
    UUID matchedExchangeUuid,
    BookingState bookingStatus,
    BookingState expectedAmendedBookingStatus
  ) {
    super(
      matchedExchangeUuid,
      bookingStatus,
      expectedAmendedBookingStatus,
      false
    );
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.of(
      createSubCheck(
        "Validate 'bookingStatus' is correct",
        this::ensureBookingStatusIsCorrect
      ),
      createSubCheck(
        "Validate 'amendedBookingStatus' is correct",
        this::ensureAmendedBookingStatusIsCorrect
      ),
      createSubCheck(
        "Validate 'carrierBookingReference' is conditionally present",
        this::ensureCarrierBookingReferenceCompliance
      ),
      createSubCheck(
        "Validate 'requestedChanges' is only present on states where it is allowed",
        requiredOrExcludedByState(PENDING_CHANGES_STATES, "requestedChanges")
      ),
      createSubCheck(
        "Validate 'reason' is only present on states where it is allowed",
        requiredOrExcludedByState(REASON_STATES, "reason")
      )
    );
  }
}
