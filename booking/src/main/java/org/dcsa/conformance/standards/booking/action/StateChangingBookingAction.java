package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.traffic.ConformanceExchange;

public abstract class StateChangingBookingAction extends BookingAction {
  public StateChangingBookingAction(String sourcePartyName, String targetPartyName, BookingAction previousAction, String actionTitle, int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus);
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateDSPFromResponsePayload(exchange);
  }
}
