package org.dcsa.conformance.standards.ebl.action;

import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standardscommons.action.BookingAndEblAction;

import java.util.Set;

public abstract class StateChangingSIAction extends EblAction {

  protected StateChangingSIAction(
      String sourcePartyName,
      String targetPartyName,
      BookingAndEblAction previousAction,
      String actionTitle,
      int expectedStatus,
      boolean isWithNotifications) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus, isWithNotifications);
  }

  protected StateChangingSIAction(
      String sourcePartyName,
      String targetPartyName,
      EblAction previousAction,
      String actionTitle,
      Set<Integer> expectedStatus,
      boolean isWithNotifications) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus, isWithNotifications);
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateDSPFromSIResponsePayload(exchange);
  }
}
