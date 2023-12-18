package org.dcsa.conformance.standards.booking.checks;

import java.util.*;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingState;

public class CarrierGetBookingPayloadResponseConformanceCheck extends AbstractCarrierPayloadConformanceCheck {

  private static final Set<String> MANDATORY_ON_CONFIRMED_BOOKING = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
    "confirmedEquipments",
    "transportPlan",
    "shipmentCutOffTimes",
    "charges",
    "carrierClauses",
    "termsAndConditions"
  )));

  public CarrierGetBookingPayloadResponseConformanceCheck(
    UUID matchedExchangeUuid,
    BookingState bookingStatus,
    BookingState expectedAmendedBookingStatus,
    boolean amendedContent
  ) {
    super(
      matchedExchangeUuid,
      HttpMessageType.RESPONSE,
      bookingStatus,
      expectedAmendedBookingStatus,
      amendedContent
    );
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    List<ConformanceCheck> checks = new ArrayList<>();
    /*
     * Checks for fields that the *Carrier* is responsible for.  That is things like "requestedChanges"
     * or the "transportPlan".  If the Shipper should conditionally provide a field, then that is
     * validation should go somewhere else.
     *
     * As an example, the Carrier does *not* get flagged for not providing "either serviceContractReference
     * OR contractQuotationReference" if the Shipper failed to provide those in the last POST/PUT call
     */
    addSubCheck(
      "Validate 'bookingStatus' is correct",
      this::ensureBookingStatusIsCorrect,
      checks::add
    );
    addSubCheck(
      "Validate 'amendedBookingStatus' is correct",
      this::ensureAmendedBookingStatusIsCorrect,
      checks::add
    );
    addSubCheck(
      "Validate 'carrierBookingReference' is conditionally present",
      this::ensureCarrierBookingReferenceCompliance,
      checks::add
    );
    addSubCheck(
      "Validate 'requestedChanges' is only present on states where it is allowed",
      requiredOrExcludedByState(PENDING_CHANGES_STATES, "requestedChanges"),
      checks::add
    );
    addSubCheck(
      "Validate 'reason' is only present on states where it is allowed",
      requiredOrExcludedByState(REASON_STATES, "reason"),
      checks::add
    );
    for (var fieldName : MANDATORY_ON_CONFIRMED_BOOKING) {
      addSubCheck("'" + fieldName + "' is required for confirmed bookings",
        requiredCarrierAttributeIfState(CONFIRMED_BOOKING_STATES, fieldName),
        checks::add
      );
    }
    addSubCheck(
      "Validate 'shipmentLocations' is present",
      this::ensureShipmentLocationsArePresent,
      checks::add
    );
    return checks.stream();
  }
}
