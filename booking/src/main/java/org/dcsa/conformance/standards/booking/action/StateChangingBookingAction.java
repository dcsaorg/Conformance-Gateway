package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.booking.party.BookingVariant;

public abstract class StateChangingBookingAction extends BookingAction {

  protected final BookingVariant bookingVariant;
  public StateChangingBookingAction(String sourcePartyName, String targetPartyName, BookingAction previousAction,
                                    String actionTitle, int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus);
    bookingVariant = BookingVariant.REGULAR;
  }

  public StateChangingBookingAction(String sourcePartyName, String targetPartyName, BookingAction previousAction,
                                    String actionTitle, int expectedStatus, BookingVariant bookingVariant) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus);
    this.bookingVariant = bookingVariant;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateDSPFromResponsePayload(exchange);
  }
}
