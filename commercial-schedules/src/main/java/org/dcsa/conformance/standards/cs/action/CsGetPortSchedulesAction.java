package org.dcsa.conformance.standards.cs.action;

import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;

public class CsGetPortSchedulesAction extends CsAction{

  private String scenarioType;
    public CsGetPortSchedulesAction(String subscriberPartyName, String publisherPartyName, ConformanceAction previousAction, JsonSchemaValidator portSchedule) {
        super(subscriberPartyName, publisherPartyName, previousAction, "GetSchedules", 200);
        this.responseSchemaValidator = responseSchemaValidator;
    }

  @Override
  public String getHumanReadablePrompt() {
    return null;
  }
}
