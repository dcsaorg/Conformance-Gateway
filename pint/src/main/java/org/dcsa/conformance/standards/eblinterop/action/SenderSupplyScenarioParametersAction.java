package org.dcsa.conformance.standards.eblinterop.action;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.standards.eblinterop.models.SenderScenarioParameters;

@Getter
@Slf4j
public class SenderSupplyScenarioParametersAction extends PintAction {

  public SenderSupplyScenarioParametersAction(
      String platformPartyName,
      String carrierPartyName,
      PintAction previousAction) {
    super(
        carrierPartyName,
        platformPartyName,
        previousAction,
        "SupplyScenarioParameters",
        -1);
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    super.handlePartyInput(partyInput);
    var ssp = SenderScenarioParameters.fromJson(partyInput.path("input"));
    this.setSsp(ssp);
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return new SenderScenarioParameters(
      "TD reference"
    ).toJson();
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Scenario details");
  }

}
