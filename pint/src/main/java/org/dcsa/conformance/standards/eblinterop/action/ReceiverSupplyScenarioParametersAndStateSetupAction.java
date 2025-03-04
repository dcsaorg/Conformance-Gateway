package org.dcsa.conformance.standards.eblinterop.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.standards.eblinterop.models.ReceiverScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.party.PintReceivingPlatform;

@Getter
@Slf4j
public class ReceiverSupplyScenarioParametersAndStateSetupAction extends PintAction {
  public static final String ACTION_PREFIX = "ReceiverScenarioParametersAndSetup";
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
        "%s(%s)".formatted(ACTION_PREFIX, scenarioClass.name()),
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
    rsp.validate();
    this.setRsp(rsp);
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Setup the system for transfer and provide the following details for the sender.");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return PintReceivingPlatform.getReceiverScenarioParameters(
      getSsp().eblPlatform(),
      scenarioClass
    ).toJson();
  }
}
