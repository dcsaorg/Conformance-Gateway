package org.dcsa.conformance.standards.eblsurrender.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.util.ReferenceGenerator;
import org.dcsa.conformance.standards.eblsurrender.party.SuppliedScenarioParameters;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Getter
public class SupplyScenarioParametersAction extends ConformanceAction {

  private SuppliedScenarioParameters suppliedScenarioParameters = null;

  public SupplyScenarioParametersAction(String carrierPartyName, ConformanceAction previousAction) {
    super(carrierPartyName, null, previousAction, "SupplyCSP[Transport Document data]");
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
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode sspNode = jsonState.get("suppliedScenarioParameters");
    if (sspNode != null) {
      suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(sspNode);
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return EblSurrenderAction.getMarkdownHumanReadablePrompt(java.util.Map.of(), "prompt-surrender-ssp.md");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    var issueToParty = exampleParty("Issue To name", "WAVE");
    var carrierParty = exampleParty("Carrier name", "WAVE");
    var surrendereeParty = exampleParty("Surrenderee name", "BOLE");
    return new SuppliedScenarioParameters(ReferenceGenerator.newReference(), issueToParty, carrierParty, surrendereeParty).toJson();
  }

  private static ObjectNode exampleParty(String partyName, String eblPlatform) {
    ObjectNode party = OBJECT_MAPPER.createObjectNode().put("partyName", partyName).put("eblPlatform", eblPlatform);
    party.putArray("identifyingCodes")
      .addObject()
      .put("codeListProvider", "DCSA")
      .put("partyCode", "PARTY-CODE");
    return party;
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInput.get("input"));
  }

}
