package org.dcsa.conformance.end.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;

public class EndorsementChainAction extends ConformanceAction {
  protected EndorsementChainAction(String sourcePartyName, String targetPartyName, ConformanceAction previousAction, String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
  }

  @Override
  public String getHumanReadablePrompt() {
    return "";
  }
}
