package org.dcsa.conformance.standards.cs.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;

public abstract class CommercialSchedulesAction extends ConformanceAction {

  protected CommercialSchedulesAction(String sourcePartyName, String targetPartyName, ConformanceAction previousAction, String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
  }
}
