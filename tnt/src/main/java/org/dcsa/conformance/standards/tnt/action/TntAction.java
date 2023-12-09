package org.dcsa.conformance.standards.tnt.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;

public abstract class TntAction extends ConformanceAction {
  protected final int expectedStatus;

  public TntAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
  }
}
