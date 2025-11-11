package org.dcsa.conformance.standards.vgm.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;

public class VgmAction extends ConformanceAction {

  protected VgmAction(
      String sourcePartyName,
      String targetPartyName,
      VgmAction previousAction,
      String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
  }

  @Override
  public String getHumanReadablePrompt() {
    return "";
  }
}
