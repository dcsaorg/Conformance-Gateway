package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standardscommons.action.BookingAndEblAction;

import java.util.Set;

/**
 * A Shipper-initiated use case where the carrier follow-up notification exchange is optional.
 */
public abstract class ShipperNotificationBookingAction extends StateChangingBookingAction {

  protected ShipperNotificationBookingAction(
    String shipperPartyName,
    String carrierPartyName,
    BookingAndEblAction previousAction,
    String actionTitle,
    int expectedStatus,
    boolean isWithNotifications) {
    super(
      shipperPartyName,
      carrierPartyName,
      previousAction,
      actionTitle,
      expectedStatus,
      isWithNotifications);
  }

  @Override
  public Set<String> completableWithoutTrafficForRoles() {
    return Set.of(BookingRole.CARRIER.getConfigName());
  }
}

