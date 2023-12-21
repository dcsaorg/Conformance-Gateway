package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;

public abstract class StateChangingBookingAction extends BookingAction {

  protected final ScenarioType scenarioType;
  public StateChangingBookingAction(String sourcePartyName, String targetPartyName, BookingAction previousAction,
                                    String actionTitle, int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus);
    scenarioType = ScenarioType.REGULAR;
  }

  public StateChangingBookingAction(String sourcePartyName, String targetPartyName, BookingAction previousAction,
                                    String actionTitle, int expectedStatus, ScenarioType scenarioType) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus);
    this.scenarioType = scenarioType;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateDSPFromResponsePayload(exchange);
  }
}
