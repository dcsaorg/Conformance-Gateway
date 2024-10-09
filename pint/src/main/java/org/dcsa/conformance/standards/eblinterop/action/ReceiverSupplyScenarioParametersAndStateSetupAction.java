package org.dcsa.conformance.standards.eblinterop.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.standards.eblinterop.models.ReceiverScenarioParameters;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Getter
@Slf4j
public class ReceiverSupplyScenarioParametersAndStateSetupAction extends PintAction {
  private final ScenarioClass scenarioClass;

  public ReceiverSupplyScenarioParametersAndStateSetupAction(
      String receivingPlatform,
      String sendingPlatform,
      PintAction previousAction,
      ScenarioClass scenarioClass) {
    super(
        receivingPlatform,
        sendingPlatform,
        previousAction,
        "ReceiverScenarioParametersAndSetup(%s)"
            .formatted(scenarioClass.name()),
      -1
      );
    this.scenarioClass = scenarioClass;
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode()
      .put("scenarioClass", this.scenarioClass.name());
    node.set("ssp", this.getSsp().toJson());
    return node;
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    super.handlePartyInput(partyInput);
    var rsp = ReceiverScenarioParameters.fromJson(partyInput.path("input"));
    this.setRsp(rsp);
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Setup the system for transfer and provide the following details for the sender.");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    var partyCode = OBJECT_MAPPER.createObjectNode()
      .put("partyCode", "some-party-code")
      .put("codeListProvider", "ZZZ");
    var receiverParty = OBJECT_MAPPER.createObjectNode();
    receiverParty.put("partyName", "Jane Doe")
      .put("eblPlatform", "BOLE")
      .putArray("identifyingCodes")
      .add(partyCode);
    return new ReceiverScenarioParameters(
      receiverParty,
      "-----BEGIN RSA PUBLIC KEY-----\n<YOUR PUBLIC SIGNING KEY HERE>\n-----END RSA PUBLIC KEY-----\n"
    ).toJson();
  }
}
