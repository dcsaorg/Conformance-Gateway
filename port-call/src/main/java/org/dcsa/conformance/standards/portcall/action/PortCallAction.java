package org.dcsa.conformance.standards.portcall.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.standards.portcall.party.DynamicScenarioParameters;

@Slf4j
@Getter
public abstract class PortCallAction extends ConformanceAction {

  public static final String DSP_TAG = "dsp";

  protected DynamicScenarioParameters dsp;

  protected PortCallAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    if (previousAction instanceof PortCallAction previousJitAction
        && previousJitAction.dsp != null) {
      dsp = previousJitAction.dsp;
    }
  }

  @Override
  public void reset() {
    super.reset();
    dsp = null;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    if (previousAction != null) {
      dsp = ((PortCallAction) previousAction).getDsp();
      jsonNode.set(DSP_TAG, dsp.toJson());
    }
    return jsonNode;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (dsp != null) {
      jsonState.set(DSP_TAG, dsp.toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode dspNode = jsonState.get(DSP_TAG);
    if (dspNode != null) {
      dsp = DynamicScenarioParameters.fromJson(dspNode);
    }
  }

  protected String getMarkdownFile(String fileName) {
    return getMarkdownFile(fileName, null);
  }

  protected String getMarkdownFile(String fileName, Map<String, String> replacements) {
    return IOToolkit.templateFileToText("/standards/jit/instructions/" + fileName, replacements);
  }
}
