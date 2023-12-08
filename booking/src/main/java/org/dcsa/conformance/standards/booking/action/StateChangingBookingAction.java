package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.traffic.ConformanceExchange;

public abstract class StateChangingBookingAction extends BookingAction {

  protected final String bookingVariant;
  public StateChangingBookingAction(String sourcePartyName, String targetPartyName, BookingAction previousAction,
                                    String actionTitle, int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus);
    bookingVariant = null;
  }

  public StateChangingBookingAction(String sourcePartyName, String targetPartyName, BookingAction previousAction,
                                    String actionTitle, int expectedStatus, String bookingVariant) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus);
    this.bookingVariant = bookingVariant;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateDSPFromResponsePayload(exchange);
  }
}
