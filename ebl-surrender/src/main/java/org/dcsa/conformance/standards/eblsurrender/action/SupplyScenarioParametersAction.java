package org.dcsa.conformance.standards.eblsurrender.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.util.ReferenceGenerator;
import org.dcsa.conformance.standards.eblsurrender.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends ConformanceAction {

  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private String response;
  private final boolean isErrorScenario;

  public SupplyScenarioParametersAction(
      String carrierPartyName,
      ConformanceAction previousAction,
      String response,
      boolean errorScenario) {
    super(carrierPartyName, null, previousAction, "SupplyTDR");
    this.response = response;
    this.isErrorScenario = errorScenario;
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
  }

  @Override
  public String getHumanReadablePrompt() {
    String responseAction = response.equals("SURR") ? "accept" : "reject";
    return isErrorScenario
        ? EblSurrenderAction.getMarkdownHumanReadablePrompt(
            Map.of(), "prompt-surrender-error-ssp.md")
        : EblSurrenderAction.getMarkdownHumanReadablePrompt(
            Map.of("RESPONSE", responseAction), "prompt-surrender-ssp.md");
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
            ReferenceGenerator.newReference(), issueToParty, carrierParty, surrendereeParty)
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
    return super.asJsonNode().put("response", response).put("errorScenario", isErrorScenario);
  }
}
