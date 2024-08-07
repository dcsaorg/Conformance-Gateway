package org.dcsa.conformance.standards.cs.action;

import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;

public class CsGetRoutingsAction extends CsAction{
    public CsGetRoutingsAction(String subscriberPartyName, String publisherPartyName, ConformanceAction previousAction, JsonSchemaValidator vesselSchedule) {
      super(subscriberPartyName, publisherPartyName, previousAction, "GetSchedules", 200);

    }

  @Override
  public String getHumanReadablePrompt() {
    return null;
  }
}
