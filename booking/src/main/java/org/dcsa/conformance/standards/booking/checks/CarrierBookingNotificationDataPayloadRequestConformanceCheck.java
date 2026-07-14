package org.dcsa.conformance.standards.booking.checks;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standardscommons.party.BookingDynamicScenarioParameters;

public class CarrierBookingNotificationDataPayloadRequestConformanceCheck
    extends AbstractCarrierPayloadConformanceCheck {

  private static final String DATA_PATH = "/data";
  private static final String BOOKING_PATH = DATA_PATH + "/booking";
  private static final String AMENDED_BOOKING_PATH = DATA_PATH + "/amendedBooking";

  private static final String DEFAULT_PREFIX = "";
  private static final String BOOKING_PREFIX = "[Booking]";
  private static final String AMENDED_BOOKING_PREFIX = "[Amended Booking]";

  private final Supplier<BookingDynamicScenarioParameters> dspSupplier;

  public CarrierBookingNotificationDataPayloadRequestConformanceCheck(
      UUID matchedExchangeUuid,
      BookingState bookingStatus,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedBookingCancellationStatus,
      Supplier<BookingDynamicScenarioParameters> dspSupplier) {
    super(
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        bookingStatus,
        expectedAmendedBookingStatus,
        expectedBookingCancellationStatus);
    this.dspSupplier = dspSupplier;
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.of(
            Stream.of(
                createSubCheck(
                    DEFAULT_PREFIX,
                    "Validate 'data.bookingStatus' is correct",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureBookingStatusIsCorrect)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "Validate 'data.amendedBookingStatus' is correct",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureAmendedBookingStatusIsCorrect)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "Validate 'data.bookingCancellationStatus' is correct",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureBookingCancellationStatusIsCorrect)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "The data.carrierBookingReference attribute in the Booking Notification must demonstrate the correct use of this conditional requirement: carrierBookingReference MUST be present, except for the booking states where it is still optional: RECEIVED, REJECTED, PENDING_UPDATE, UPDATE_RECEIVED, or CANCELLED",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureCarrierBookingReferenceCompliance)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "The data.feedbacks attribute in the Booking Notification must be provided when bookingStatus is PENDING_UPDATE or PENDING_AMENDMENT; it is optional for all other booking statuses",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureFeedbacksIsPresent)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "The feedbacks.severity attribute must demonstrate the correct use of a feedback severity code: INFO, WARN, or ERROR",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureFeedbackSeverityCompliance)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "The feedbacks.code attribute must demonstrate the correct use of a feedback code: INFORMATIONAL_MESSAGE, PROPERTY_WILL_BE_IGNORED, PROPERTY_VALUE_MUST_CHANGE, PROPERTY_VALUE_HAS_BEEN_CHANGED, PROPERTY_VALUE_MAY_CHANGE, or PROPERTY_HAS_BEEN_DELETED",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureFeedbackCodeCompliance))),
            createFullNotificationChecksAt(BOOKING_PATH, BOOKING_PREFIX),
            createFullNotificationChecksAt(AMENDED_BOOKING_PATH, AMENDED_BOOKING_PREFIX))
        .flatMap(Function.identity());
  }

  private Stream<ConformanceCheck> createFullNotificationChecksAt(String jsonPath, String prefix) {
    return BookingChecks.fullPayloadChecks(
            dspSupplier,
            expectedBookingStatus,
            expectedAmendedBookingStatus,
            expectedBookingCancellationStatus)
        .stream()
        .map(
            jsonContentCheck ->
                createSubCheck(
                    prefix,
                    jsonContentCheck.description(),
                    jsonContentCheck.isRelevant(),
                    jsonPath,
                    at(jsonPath, jsonContentCheck::validate)));
  }
}
