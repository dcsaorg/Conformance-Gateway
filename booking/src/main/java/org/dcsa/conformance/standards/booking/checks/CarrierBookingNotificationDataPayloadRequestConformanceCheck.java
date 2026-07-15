package org.dcsa.conformance.standards.booking.checks;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
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
                    "The time attribute in the Booking Notification must demonstrate the correct use of this conditional requirement: Timestamp of when the occurrence happened. If the time of the occurrence cannot be determined then this attribute MAY be set to some other time (such as the current time) by the CloudEvents producer, however all producers for the same source MUST be consistent in this respect. In other words, either they all use the actual time of the occurrence or they all use the same algorithm to determine the value used",
                    "",
                    ignored ->
                        ConformanceCheckResult.withRelevance(
                            Set.of(ConformanceError.irrelevant()))),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "[Use case] The combination of data.bookingStatus, data.amendedBookingStatus and data.bookingCancellationStatus must match the active use case: data.bookingStatus must equal %s"
                        .formatted(expectedBookingStatus.name()),
                    DATA_PATH,
                    at(DATA_PATH, this::ensureBookingStatusIsCorrect)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "[Use case] The combination of data.bookingStatus, data.amendedBookingStatus and data.bookingCancellationStatus must match the active use case: data.amendedBookingStatus must %s"
                        .formatted(
                            expectedAmendedBookingStatus == null
                                ? "be absent"
                                : "equal " + expectedAmendedBookingStatus.name()),
                    DATA_PATH,
                    at(DATA_PATH, this::ensureAmendedBookingStatusIsCorrect)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "[Use case] The combination of data.bookingStatus, data.amendedBookingStatus and data.bookingCancellationStatus must match the active use case: data.bookingCancellationStatus must %s"
                        .formatted(
                            expectedBookingCancellationStatus == null
                                ? "be absent"
                                : "equal " + expectedBookingCancellationStatus.name()),
                    DATA_PATH,
                    at(DATA_PATH, this::ensureBookingCancellationStatusIsCorrect)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "(if included) The data.amendedBookingStatus attribute in the Booking Notification must only be used when the standard allows it: amendedBookingStatus and bookingCancellationStatus MUST NOT be present unless required by the applicable use case",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureAmendedBookingStatusUsageIsCorrect)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "(if included) The data.bookingCancellationStatus attribute in the Booking Notification must only be used when the standard allows it: amendedBookingStatus and bookingCancellationStatus MUST NOT be present unless required by the applicable use case",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureBookingCancellationStatusUsageIsCorrect)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "The data.bookingStatus attribute in the Booking Notification must demonstrate the correct use of a booking status code: RECEIVED, PENDING_UPDATE, UPDATE_RECEIVED, CONFIRMED, PENDING_AMENDMENT, REJECTED, DECLINED, CANCELLED, or COMPLETED",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureBookingStatusCodeCompliance)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "The data.amendedBookingStatus attribute in the Booking Notification must demonstrate the correct use of an amended booking status code: AMENDMENT_RECEIVED, AMENDMENT_CONFIRMED, AMENDMENT_DECLINED, or AMENDMENT_CANCELLED",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureAmendedBookingStatusCodeCompliance)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "The data.bookingCancellationStatus attribute in the Booking Notification must demonstrate the correct use of a booking cancellation status code: CANCELLATION_RECEIVED, CANCELLATION_DECLINED, or CANCELLATION_CONFIRMED",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureBookingCancellationStatusCodeCompliance)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "The data.carrierBookingReference / data.carrierBookingRequestReference attributes in the Booking Notification must demonstrate the correct use of the carrierBookingRequestReference or carrierBookingReference attribute by providing at least one of them",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureAtLeastOneCarrierReferenceIsPresent)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "The data.carrierBookingReference / data.carrierBookingRequestReference attributes in the Booking Notification must demonstrate the correct use of the carrierBookingRequestReference or carrierBookingReference attribute: carrierBookingRequestReference MUST equal the reference established for the scenario, or carrierBookingReference MUST equal the reference established for the scenario",
                    DATA_PATH,
                    at(DATA_PATH, this::ensureCarrierReferenceMatchesScenario)),
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
    return BookingChecks.nestedNotificationPayloadChecks(dspSupplier)
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

  private ConformanceCheckResult ensureCarrierReferenceMatchesScenario(
      com.fasterxml.jackson.databind.JsonNode data) {
    var dsp = dspSupplier.get();
    boolean cbrrMatches =
        dsp.carrierBookingRequestReference() != null
            && dsp.carrierBookingRequestReference()
                .equals(data.path("carrierBookingRequestReference").asText(""));
    boolean cbrMatches =
        dsp.carrierBookingReference() != null
            && dsp.carrierBookingReference()
                .equals(data.path("carrierBookingReference").asText(""));
    if (cbrrMatches || cbrMatches) {
      return ConformanceCheckResult.simple(Set.of());
    }
    return ConformanceCheckResult.simple(
        Set.of(
            "Neither carrierBookingRequestReference nor carrierBookingReference matches the reference established for the scenario"));
  }
}
