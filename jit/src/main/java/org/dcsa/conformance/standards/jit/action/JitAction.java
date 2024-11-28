package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.jit.party.DynamicScenarioParameters;

@Slf4j
public abstract class JitAction extends ConformanceAction {

  @Getter protected DynamicScenarioParameters dsp;

  protected JitAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    if (previousAction instanceof JitAction previousJitAction && previousJitAction.dsp != null) {
      dsp = previousJitAction.dsp;
    }
  }

  @Override
  public void reset() {
    super.reset();
    if (previousAction != null) {
      dsp = null;
    }
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (dsp != null) {
      jsonState.set("dsp", dsp.toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode dspNode = jsonState.get("dsp");
    if (dspNode != null) {
      dsp = DynamicScenarioParameters.fromJson(dspNode);
    }
  }
}
