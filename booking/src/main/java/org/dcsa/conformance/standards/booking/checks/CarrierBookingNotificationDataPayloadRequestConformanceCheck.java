package org.dcsa.conformance.standards.booking.checks;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standards.booking.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

public class CarrierBookingNotificationDataPayloadRequestConformanceCheck
    extends AbstractCarrierPayloadConformanceCheck {

  private static final String DATA_PATH = "/data";
  private static final String BOOKING_PATH = DATA_PATH + "/booking";
  private static final String AMENDED_BOOKING_PATH = DATA_PATH + "/amendedBooking";

  private static final String DEFAULT_PREFIX = "";
  private static final String BOOKING_PREFIX = "[Booking]";
  private static final String AMENDED_BOOKING_PREFIX = "[Amended Booking]";

  private final Supplier<CarrierScenarioParameters> cspSupplier;
  private final Supplier<DynamicScenarioParameters> dspSupplier;

  public CarrierBookingNotificationDataPayloadRequestConformanceCheck(
      UUID matchedExchangeUuid,
      BookingState bookingStatus,
      Supplier<CarrierScenarioParameters> cspSupplier,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    super(matchedExchangeUuid, HttpMessageType.REQUEST, bookingStatus);
    this.cspSupplier = cspSupplier;
    this.dspSupplier = dspSupplier;
  }

  public CarrierBookingNotificationDataPayloadRequestConformanceCheck(
      UUID matchedExchangeUuid,
      BookingState bookingStatus,
      BookingState expectedAmendedBookingStatus,
      Supplier<CarrierScenarioParameters> cspSupplier,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    super(
        matchedExchangeUuid, HttpMessageType.REQUEST, bookingStatus, expectedAmendedBookingStatus);
    this.cspSupplier = cspSupplier;
    this.dspSupplier = dspSupplier;
  }

  public CarrierBookingNotificationDataPayloadRequestConformanceCheck(
      UUID matchedExchangeUuid,
      BookingState bookingStatus,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedBookingCancellationStatus,
      Supplier<CarrierScenarioParameters> cspSupplier,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    super(
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        bookingStatus,
        expectedAmendedBookingStatus,
        expectedBookingCancellationStatus,
        false);
    this.cspSupplier = cspSupplier;
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
            expectedAmendedBookingStatus != null
                ? createFullNotificationChecksAt(AMENDED_BOOKING_PATH, AMENDED_BOOKING_PREFIX)
                : Stream.<ConformanceCheck>empty())
        .flatMap(Function.identity());
  }

  private Stream<ConformanceCheck> createFullNotificationChecksAt(String jsonPath, String prefix) {
    return BookingChecks.fullPayloadChecks(
            cspSupplier,
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
                    at(jsonPath, jsonContentCheck::validate)));
  }
}
