package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.standards.booking.party.BookingRole;

import java.util.Set;

/** A Carrier state-changing use case whose booking notification exchange is optional. */
public abstract class CarrierNotificationBookingAction extends StateChangingBookingAction {

  protected CarrierNotificationBookingAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    String actionTitle,
    int expectedStatus,
    boolean isWithNotifications) {
    super(
      carrierPartyName,
      shipperPartyName,
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
