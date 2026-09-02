package org.dcsa.conformance.standards.booking.checks;

import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.KeywordDataset;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standardscommons.party.BookingDynamicScenarioParameters;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CarrierBookingNotificationDataPayloadRequestConformanceCheck
  extends AbstractCarrierPayloadConformanceCheck {

  private static final String DATA_PATH = "/data";
  private static final String BOOKING_PATH = DATA_PATH + "/booking";
  private static final String AMENDED_BOOKING_PATH = DATA_PATH + "/amendedBooking";

  private static final String DATA = "data";
  private static final String BOOKING_STATUS = "bookingStatus";
  private static final String AMENDED_BOOKING_STATUS = "amendedBookingStatus";
  private static final String BOOKING_CANCELLATION_STATUS = "bookingCancellationStatus";
  private static final String CARRIER_BOOKING_REFERENCE = "carrierBookingReference";
  private static final String CARRIER_BOOKING_REQUEST_REFERENCE = "carrierBookingRequestReference";
  private static final String FEEDBACKS = "feedbacks";
  private static final String SEVERITY = "severity";
  private static final String CODE = "code";

  private static final String DEFAULT_PREFIX = "";
  private static final String BOOKING_PREFIX = "[Booking]";
  private static final String AMENDED_BOOKING_PREFIX = "[Amended Booking]";

  private final Supplier<BookingDynamicScenarioParameters> dspSupplier;

  private static String jsonPath(String... segments) {
    return "'%s'".formatted(String.join(".", segments));
  }

  private static String datasetValues(KeywordDataset dataset) {
    return String.join(", ", dataset.values());
  }

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

  public CarrierBookingNotificationDataPayloadRequestConformanceCheck(
    UUID matchedExchangeUuid,
    CarrierStatusScenario carrierStatusScenario,
    Supplier<BookingDynamicScenarioParameters> dspSupplier) {
    super(
      matchedExchangeUuid,
      HttpMessageType.REQUEST,
      carrierStatusScenario.expectedBookingStatus(),
      carrierStatusScenario);
    this.dspSupplier = dspSupplier;
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.of(
        Stream.of(
          createSubCheck(
            DEFAULT_PREFIX,
            "[Scenario] The combination of %s, %s and %s must match the active scenario: %s must %s"
              .formatted(
                jsonPath(DATA, BOOKING_STATUS),
                jsonPath(DATA, AMENDED_BOOKING_STATUS),
                jsonPath(DATA, BOOKING_CANCELLATION_STATUS),
                jsonPath(DATA, BOOKING_STATUS),
                carrierStatusScenario.bookingStatusExpectation()),
            DATA_PATH,
            at(DATA_PATH, this::ensureBookingStatusIsCorrect)),
          createSubCheck(
            DEFAULT_PREFIX,
            "[Scenario] The combination of %s, %s and %s must match the active scenario: %s must %s"
              .formatted(
                jsonPath(DATA, BOOKING_STATUS),
                jsonPath(DATA, AMENDED_BOOKING_STATUS),
                jsonPath(DATA, BOOKING_CANCELLATION_STATUS),
                jsonPath(DATA, AMENDED_BOOKING_STATUS),
                carrierStatusScenario.amendedBookingStatusExpectation()),
            DATA_PATH,
            at(DATA_PATH, this::ensureAmendedBookingStatusIsCorrect)),
          createSubCheck(
            DEFAULT_PREFIX,
            "[Scenario] The combination of %s, %s and %s must match the active scenario: %s must %s"
              .formatted(
                jsonPath(DATA, BOOKING_STATUS),
                jsonPath(DATA, AMENDED_BOOKING_STATUS),
                jsonPath(DATA, BOOKING_CANCELLATION_STATUS),
                jsonPath(DATA, BOOKING_CANCELLATION_STATUS),
                carrierStatusScenario.bookingCancellationStatusExpectation()),
            DATA_PATH,
            at(DATA_PATH, this::ensureBookingCancellationStatusIsCorrect)),
          createSubCheck(
            DEFAULT_PREFIX,
            "[Scenario] (if included) The %s attribute in the Booking Notification must only be used when the standard allows it: %s and %s MUST NOT be present unless required by the applicable scenario"
              .formatted(
                jsonPath(DATA, AMENDED_BOOKING_STATUS),
                jsonPath(DATA, AMENDED_BOOKING_STATUS),
                jsonPath(DATA, BOOKING_CANCELLATION_STATUS)),
            DATA_PATH,
            at(DATA_PATH, this::ensureAmendedBookingStatusUsageIsCorrect)),
          createSubCheck(
            DEFAULT_PREFIX,
            "[Scenario] (if included) The %s attribute in the Booking Notification must only be used when the standard allows it: %s and %s MUST NOT be present unless required by the applicable scenario"
              .formatted(
                jsonPath(DATA, BOOKING_CANCELLATION_STATUS),
                jsonPath(DATA, AMENDED_BOOKING_STATUS),
                jsonPath(DATA, BOOKING_CANCELLATION_STATUS)),
            DATA_PATH,
            at(DATA_PATH, this::ensureBookingCancellationStatusUsageIsCorrect)),
          createSubCheck(
            DEFAULT_PREFIX,
            "The %s attribute in the Booking Notification must demonstrate the correct use of a booking status code: %s"
              .formatted(
                jsonPath(DATA, BOOKING_STATUS),
                datasetValues(BookingDataSets.BOOKING_STATUS)),
            DATA_PATH,
            at(DATA_PATH, this::ensureBookingStatusCodeCompliance)),
          createSubCheck(
            DEFAULT_PREFIX,
            "The %s attribute in the Booking Notification must demonstrate the correct use of an amended booking status code: %s"
              .formatted(
                jsonPath(DATA, AMENDED_BOOKING_STATUS),
                datasetValues(BookingDataSets.AMENDED_BOOKING_STATUS)),
            DATA_PATH,
            at(DATA_PATH, this::ensureAmendedBookingStatusCodeCompliance)),
          createSubCheck(
            DEFAULT_PREFIX,
            "The %s attribute in the Booking Notification must demonstrate the correct use of a booking cancellation status code: %s"
              .formatted(
                jsonPath(DATA, BOOKING_CANCELLATION_STATUS),
                datasetValues(BookingDataSets.BOOKING_CANCELLATION_STATUS)),
            DATA_PATH,
            at(DATA_PATH, this::ensureBookingCancellationStatusCodeCompliance)),
          createSubCheck(
            DEFAULT_PREFIX,
            "The %s / %s attributes in the Booking Notification must demonstrate the correct use of the %s or %s attribute by providing at least one of them"
              .formatted(
                jsonPath(DATA, CARRIER_BOOKING_REFERENCE),
                jsonPath(DATA, CARRIER_BOOKING_REQUEST_REFERENCE),
                jsonPath(DATA, CARRIER_BOOKING_REQUEST_REFERENCE),
                jsonPath(DATA, CARRIER_BOOKING_REFERENCE)),
            DATA_PATH,
            at(DATA_PATH, this::ensureAtLeastOneCarrierReferenceIsPresent)),
          createSubCheck(
            DEFAULT_PREFIX,
            "[Scenario] The %s / %s attributes in the Booking Notification must demonstrate the correct use of the %s or %s attribute: %s MUST equal the reference established for the scenario, or %s MUST equal the reference established for the scenario"
              .formatted(
                jsonPath(DATA, CARRIER_BOOKING_REFERENCE),
                jsonPath(DATA, CARRIER_BOOKING_REQUEST_REFERENCE),
                jsonPath(DATA, CARRIER_BOOKING_REQUEST_REFERENCE),
                jsonPath(DATA, CARRIER_BOOKING_REFERENCE),
                jsonPath(DATA, CARRIER_BOOKING_REQUEST_REFERENCE),
                jsonPath(DATA, CARRIER_BOOKING_REFERENCE)),
            DATA_PATH,
            at(DATA_PATH, this::ensureCarrierReferenceMatchesScenario)),
          createSubCheck(
            DEFAULT_PREFIX,
            "[Scenario] The %s attribute in the Booking Notification must demonstrate the correct use of this conditional requirement: %s MUST be present, except for the booking states where it is still optional: %s"
              .formatted(
                jsonPath(DATA, CARRIER_BOOKING_REFERENCE),
                jsonPath(DATA, CARRIER_BOOKING_REFERENCE),
                datasetValues(
                  BookingDataSets.CARRIER_BOOKING_REFERENCE_OPTIONAL_STATES)),
            DATA_PATH,
            at(DATA_PATH, this::ensureCarrierBookingReferenceCompliance)),
          createSubCheck(
            DEFAULT_PREFIX,
            "The %s attribute in the Booking Notification must be provided when %s is PENDING_UPDATE or PENDING_AMENDMENT; it is optional for all other booking statuses"
              .formatted(
                jsonPath(DATA, FEEDBACKS), jsonPath(DATA, BOOKING_STATUS)),
            DATA_PATH,
            at(DATA_PATH, this::ensureFeedbacksIsPresent)),
          createSubCheck(
            DEFAULT_PREFIX,
            "The %s attribute must demonstrate the correct use of a feedback severity code: %s"
              .formatted(
                jsonPath(DATA, FEEDBACKS, SEVERITY),
                datasetValues(BookingDataSets.FEEDBACKS_SEVERITY)),
            DATA_PATH,
            at(DATA_PATH, this::ensureFeedbackSeverityCompliance)),
          createSubCheck(
            DEFAULT_PREFIX,
            "The %s attribute must demonstrate the correct use of a feedback code: %s"
              .formatted(
                jsonPath(DATA, FEEDBACKS, CODE),
                datasetValues(BookingDataSets.FEEDBACKS_CODE)),
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
