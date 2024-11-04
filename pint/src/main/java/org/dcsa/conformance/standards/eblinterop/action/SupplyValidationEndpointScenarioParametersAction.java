package org.dcsa.conformance.standards.eblinterop.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class SupplyValidationEndpointScenarioParametersAction extends PintAction {

  public SupplyValidationEndpointScenarioParametersAction(
      String platformPartyName,
      String carrierPartyName,
      PintAction previousAction) {
    super(
        carrierPartyName,
        platformPartyName,
        previousAction,
        "SupplyValidationEndpointScenarioParameters",
        -1);
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    super.handlePartyInput(partyInput);
    setDsp(getDsp().withReceiverValidation(partyInput.path("input")));
  }

  public static ObjectNode getJsonForPrompt() {
    var partyDef = OBJECT_MAPPER.createObjectNode();
    partyDef.put("codeListProvider", "ZZZ")
      .put("partyCode", "valid-party")
      .put("codeListName", "CTK");
    return partyDef;
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return getJsonForPrompt();
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Provide parameters for the receiver validation endpoint");
  }

}
