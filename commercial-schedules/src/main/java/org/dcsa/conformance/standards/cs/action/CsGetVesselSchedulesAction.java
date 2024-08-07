package org.dcsa.conformance.standards.cs.action;

import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;

public class CsGetVesselSchedulesAction extends CsAction{
  public CsGetVesselSchedulesAction(String subscriberPartyName, String publisherPartyName, ConformanceAction previousAction, JsonSchemaValidator vesselSchedule) {
    super(publisherPartyName, null, null,
      "SupplyCSP [%s]");
  }

  @Override
  public String getHumanReadablePrompt() {
    return null;
  }
}
