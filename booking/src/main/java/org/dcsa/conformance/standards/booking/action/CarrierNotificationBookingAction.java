package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.standards.booking.party.BookingRole;

import java.util.Set;

/** A Carrier state-changing use case represented solely by an optional booking notification. */
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
  public Set<String> skippableForRoles() {
    return Set.of(BookingRole.CARRIER.getConfigName());
  }
}

