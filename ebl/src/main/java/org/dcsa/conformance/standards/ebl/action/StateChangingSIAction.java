package org.dcsa.conformance.standards.ebl.action;

import org.dcsa.conformance.core.traffic.ConformanceExchange;

public abstract class StateChangingSIAction extends EblAction {
  protected StateChangingSIAction(String sourcePartyName, String targetPartyName, EblAction previousAction, String actionTitle, int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus);
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateDSPFromResponsePayload(exchange);
  }
}
