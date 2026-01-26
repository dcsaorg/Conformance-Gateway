package org.dcsa.conformance.end.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;

public class CarrierGetEndorsementChainAction extends EndorsementChainAction{
  protected CarrierGetEndorsementChainAction(String sourcePartyName, String targetPartyName, ConformanceAction previousAction, String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
  }
}
