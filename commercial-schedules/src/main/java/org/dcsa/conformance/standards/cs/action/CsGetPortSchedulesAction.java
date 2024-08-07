package org.dcsa.conformance.standards.cs.action;

import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;

public class CsGetPortSchedulesAction extends CsAction{

  private String scenarioType;
    public CsGetPortSchedulesAction(String subscriberPartyName, String publisherPartyName, ConformanceAction previousAction, JsonSchemaValidator portSchedule) {
      super(publisherPartyName, null, null,
        "SupplyCSP [%s]");
      this.scenarioType = scenarioType;
    }

  @Override
  public String getHumanReadablePrompt() {
    return null;
  }
}
