package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.checks.CarrierStatusScenario;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.Set;

public class ShipperGetBookingSkippableAction extends ShipperGetBookingAction {

  public ShipperGetBookingSkippableAction(String carrierPartyName, String shipperPartyName, BookingAction previousAction, BookingState expectedBookingStatus, BookingState expectedAmendedBookingStatus, BookingCancellationState expectedCancelledBookingStatus, JsonSchemaValidator responseSchemaValidator, boolean requestAmendedStatus) {
    super(carrierPartyName, shipperPartyName, previousAction, expectedBookingStatus, expectedAmendedBookingStatus, expectedCancelledBookingStatus, responseSchemaValidator, requestAmendedStatus);
  }

  public ShipperGetBookingSkippableAction(String carrierPartyName, String shipperPartyName, BookingAction previousAction, CarrierStatusScenario carrierStatusScenario, JsonSchemaValidator responseSchemaValidator, boolean requestAmendedContent) {
    super(carrierPartyName, shipperPartyName, previousAction, carrierStatusScenario, responseSchemaValidator, requestAmendedContent);
  }

  @Override
  public Set<String> skippableForRoles() {
    return Set.of(BookingRole.SHIPPER.getConfigName());
  }
}
