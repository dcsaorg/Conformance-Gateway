package org.dcsa.conformance.standards.eblsurrender.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.eblsurrender.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends ConformanceAction {
  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private String response;
  private String eblType;

  public SupplyScenarioParametersAction(
      String carrierPartyName, ConformanceAction previousAction, String response, String eblType) {
    super(carrierPartyName, null, previousAction, "SupplyTDR[%s]".formatted(eblType));
    this.response = response;
    this.eblType = eblType;
  }

  @Override
  public void reset() {
    super.reset();
    suppliedScenarioParameters = null;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (suppliedScenarioParameters != null) {
      jsonState.set("suppliedScenarioParameters", suppliedScenarioParameters.toJson());
    }
    jsonState.put("response", response);
    jsonState.put("eblType", eblType);
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode sspNode = jsonState.get("suppliedScenarioParameters");
    if (sspNode != null) {
      suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(sspNode);
    }
    response = jsonState.get("response").asText();
    eblType = jsonState.get("eblType").asText();
  }

  @Override
  public String getHumanReadablePrompt() {
    String responseAction = response.equals("SURR") ? "accept" : "reject";
    return EblSurrenderAction.getMarkdownHumanReadablePrompt(
        Map.of(
            "EBL_TYPE", eblType,
            "RESPONSE", responseAction),
        "prompt-surrender-ssp.md");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    String eblPlatform = response.equals("SURR") ? "WAVE" : "WAVER";
    var issueToParty = OBJECT_MAPPER.createObjectNode();
    issueToParty.put("partyName", "Issue To name").put("eblPlatform", eblPlatform);
    var carrierParty = OBJECT_MAPPER.createObjectNode();
    carrierParty.put("partyName", "Carrier name").put("eblPlatform", "WAVE");

    var surrendereeParty = OBJECT_MAPPER.createObjectNode();
    surrendereeParty.put("partyName", "Surrenderee name").put("eblPlatform", "BOLE");
    return new SuppliedScenarioParameters(
            UUID.randomUUID().toString().replace("-", "").substring(0, 20),
            issueToParty,
            carrierParty,
            surrendereeParty)
        .toJson();
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInput.get("input"));
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("eblType", eblType).put("response", response);
  }
}
