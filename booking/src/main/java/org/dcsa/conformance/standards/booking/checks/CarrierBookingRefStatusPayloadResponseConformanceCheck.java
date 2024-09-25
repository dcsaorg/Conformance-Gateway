package org.dcsa.conformance.standards.booking.checks;

import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.*;
import java.util.stream.Stream;

public class CarrierBookingRefStatusPayloadResponseConformanceCheck extends AbstractCarrierPayloadConformanceCheck {

  public CarrierBookingRefStatusPayloadResponseConformanceCheck(UUID matchedExchangeUuid, BookingState bookingStatus) {
    super(matchedExchangeUuid, HttpMessageType.RESPONSE, bookingStatus);
  }

  public CarrierBookingRefStatusPayloadResponseConformanceCheck(UUID matchedExchangeUuid, BookingState bookingStatus, BookingState amendedBookingStatus) {
    super(matchedExchangeUuid, HttpMessageType.RESPONSE, bookingStatus, amendedBookingStatus, null,false);
  }

  public CarrierBookingRefStatusPayloadResponseConformanceCheck(
    UUID matchedExchangeUuid,
    BookingState bookingStatus,
    BookingState expectedAmendedBookingStatus,
    BookingCancellationState expectedBookingCancellationState
  ) {
    super(
      matchedExchangeUuid,
      HttpMessageType.RESPONSE,
      bookingStatus,
      expectedAmendedBookingStatus,
      expectedBookingCancellationState,
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
        "Validate 'feedbacks' is only present on states where it is allowed",
        requiredOrExcludedByState(PENDING_CHANGES_STATES, CANCELLATION_REASON_STATES,"feedbacks")
      ),
      createSubCheck(
        "Validate 'reason' is only present on states where it is allowed",
        reasonFieldRequiredForCancellation(REASON_STATES, CANCELLATION_REASON_STATES, "reason")
      )
    );
  }
}
