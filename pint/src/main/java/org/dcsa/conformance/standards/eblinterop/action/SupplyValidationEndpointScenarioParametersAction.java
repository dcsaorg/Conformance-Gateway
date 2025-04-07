package org.dcsa.conformance.standards.eblinterop.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.JsonSchemaValidator;

@Getter
@Slf4j
public class SupplyValidationEndpointScenarioParametersAction extends PintAction {

  private final boolean isValid;

  public SupplyValidationEndpointScenarioParametersAction(
      String platformPartyName,
      String carrierPartyName,
      PintAction previousAction,
      boolean isValid) {
    super(
        carrierPartyName,
        platformPartyName,
        previousAction,
        "SupplyValidationEndpointScenarioParameters",
        -1);
    this.isValid = isValid;
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

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode();
    node.put("isValid", isValid);
    return node;
  }

  public static ObjectNode getJsonForPrompt(boolean isValid) {
    var partyDef = OBJECT_MAPPER.createObjectNode();
    partyDef.put("codeListProvider", "ZZZ")
      .put("partyCode", isValid ? "valid-party" : "invalid-party")
      .put("codeListName", "CTK");
    return partyDef;
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return getJsonForPrompt(isValid);
  }

  @Override
  public String getHumanReadablePrompt() {
    if (isValid) {
      return ("Provide parameters receiver validation endpoint that will not match a party");
    }
    return ("Provide parameters for the receiver validation endpoint that matches a party");
  }

}
