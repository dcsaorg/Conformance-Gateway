package org.dcsa.conformance.standards.portcall.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

public class PortCallAction extends ConformanceAction {

  @Override
  public String getHumanReadablePrompt() {
    return "";
  }

  //private final OverwritingReference<DynamicScenarioParameters> dsp;

  protected PortCallAction(
    String sourcePartyName, String targetPartyName, PortCallAction previousAction, String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
   /* this.dsp =
      previousAction == null
        ? new OverwritingReference<>(null, new DynamicScenarioParameters(null, null))
        : new OverwritingReference<>(previousAction.dsp, null);*/
  }


  @Override
  public void reset() {
    super.reset();
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);

  }

}
