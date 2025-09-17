package org.dcsa.conformance.standards.booking.checks;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingDynamicScenarioParameters;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standards.booking.party.EblDynamicScenarioParameters;

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
      Supplier<BookingDynamicScenarioParameters> dspSupplier) {
    super(matchedExchangeUuid, HttpMessageType.REQUEST, bookingStatus);
    this.dspSupplier = dspSupplier;
  }

  public CarrierBookingNotificationDataPayloadRequestConformanceCheck(
      UUID matchedExchangeUuid,
      BookingState bookingStatus,
      BookingState expectedAmendedBookingStatus,
      Supplier<BookingDynamicScenarioParameters> dspSupplier) {
    super(
        matchedExchangeUuid, HttpMessageType.REQUEST, bookingStatus, expectedAmendedBookingStatus);
    this.dspSupplier = dspSupplier;
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
        expectedBookingCancellationStatus,
        false);
    this.dspSupplier = dspSupplier;
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.of(
            Stream.of(
                createSubCheck(
                    DEFAULT_PREFIX,
                    "Validate 'data.bookingStatus' is correct",
                    at(DATA_PATH, this::ensureBookingStatusIsCorrect)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "Validate 'data.amendedBookingStatus' is correct",
                    at(DATA_PATH, this::ensureAmendedBookingStatusIsCorrect)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "Validate 'data.bookingCancellationStatus' is correct",
                    at(DATA_PATH, this::ensureBookingCancellationStatusIsCorrect)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "Validate 'data.carrierBookingReference' is conditionally present",
                    at(DATA_PATH, this::ensureCarrierBookingReferenceCompliance)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "Validate 'data.feedbacks' is present for booking states where it is required",
                    at(DATA_PATH, this::ensureFeedbacksIsPresent)),
                createSubCheck(
                    DEFAULT_PREFIX,
                    "Validate 'data.feedbacks' severity and code are valid",
                    at(DATA_PATH, this::ensureFeedbackSeverityAndCodeCompliance))),
            createFullNotificationChecksAt(BOOKING_PATH, BOOKING_PREFIX),
            createFullNotificationChecksAt(AMENDED_BOOKING_PATH, AMENDED_BOOKING_PREFIX))
        .flatMap(Function.identity());
  }

  private Stream<ConformanceCheck> createFullNotificationChecksAt(String jsonPath, String prefix) {
    return BookingChecks.fullPayloadChecks(
            dspSupplier,
            expectedBookingStatus,
            expectedAmendedBookingStatus,
            expectedBookingCancellationStatus,
            amendedContent)
        .stream()
        .map(
            jsonContentCheck ->
                createSubCheck(
                    prefix,
                    jsonContentCheck.description(),
                    at(
                        jsonPath,
                        jsonNode -> {
                          if (jsonNode.isMissingNode() || jsonNode.isEmpty()) {
                            return Set.of();
                          }
                          return jsonContentCheck.validate(jsonNode);
                        })));
  }
}
