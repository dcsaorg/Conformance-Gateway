package org.dcsa.conformance.standards.ovs.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;

public abstract class OvsAction extends ConformanceAction {
  protected final int expectedStatus;

  public OvsAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
  }
}
