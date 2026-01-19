package org.dcsa.conformance.standards.tnt.v300.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;

public abstract class TntAction extends ConformanceAction {

  protected TntAction(
      String sourcePartyName,
      String targetPartyName,
      TntAction previousAction,
      String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
  }

  @Override
  public String getHumanReadablePrompt() {
    return "";
  }
}
