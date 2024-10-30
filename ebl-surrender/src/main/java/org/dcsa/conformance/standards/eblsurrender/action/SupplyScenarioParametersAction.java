package org.dcsa.conformance.standards.eblsurrender.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.eblsurrender.party.SuppliedScenarioParameters;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Getter
public class SupplyScenarioParametersAction extends ConformanceAction {
  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private String response;
  private String eblType;

  public SupplyScenarioParametersAction(String carrierPartyName, ConformanceAction previousAction, String response, String eblType) {
    super(carrierPartyName, null, previousAction, "SupplyTDR");
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
    jsonState.put("response",response);
    jsonState.put("eblType",eblType);
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
    return "Use the following format to provide the transport document reference and additional info "
        + "of a"+ eblType +"for which your party can accept a surrender request:";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    var issueToParty = OBJECT_MAPPER.createObjectNode();
    issueToParty.put("partyName", "Issue To name")
      .put("eblPlatform", "WAVE");
    var carrierParty = OBJECT_MAPPER.createObjectNode();
    carrierParty.put("partyName", "Carrier name")
      .put("eblPlatform", "WAVE");

    var surrendereeParty = OBJECT_MAPPER.createObjectNode();
    surrendereeParty.put("partyName", "Surrenderee name")
      .put("eblPlatform", "BOLE");
    return new SuppliedScenarioParameters(
      UUID.randomUUID().toString().replace("-", "").substring(0, 20),
      issueToParty,
      carrierParty,
      surrendereeParty
    )
        .toJson();
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    super.handlePartyInput(partyInput);
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInput.get("input"));
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("eblType", eblType)
      .put("response", response);
  }
}
