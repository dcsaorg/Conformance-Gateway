package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;

import java.util.Set;

public abstract class StateChangingBookingAction extends BookingAction {
  public StateChangingBookingAction(String sourcePartyName, String targetPartyName, BookingAction previousAction,
                                    String actionTitle, int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus);
  }

  public StateChangingBookingAction(String sourcePartyName, String targetPartyName, BookingAction previousAction,
                                    String actionTitle, Set<Integer> expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus);
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateDSPFromResponsePayload(exchange);
  }
}
