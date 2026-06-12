package org.dcsa.conformance.standards.eblsurrender.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderCarrier;

import java.util.Map;

@Getter
public class SupplyScenarioParametersErrorAction extends ConformanceAction {

  private JsonNode inputBody = null;

  public SupplyScenarioParametersErrorAction(String sourcePartyName) {
    super(sourcePartyName, null, null, "SupplyTDR");
  }

  @Override
  public void reset() {
    super.reset();
    inputBody = null;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (inputBody != null) {
      jsonState.set("inputBody", inputBody);
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode sspNode = jsonState.get("inputBody");
    if (sspNode != null) {
      inputBody = sspNode;
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return EblSurrenderAction.getMarkdownHumanReadablePrompt(Map.of(), "prompt-surrender-error-ssp.md");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return EblSurrenderCarrier.getExampleJsonBody();
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    inputBody = partyInput.get("input");
  }

  public Map<String, Boolean> getExpectedInputAttributes() {
    return Map.of();
  }
}
