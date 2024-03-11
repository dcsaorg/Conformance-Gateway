package org.dcsa.conformance.standards.eblinterop.action;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.standards.eblinterop.models.SenderScenarioParameters;

@Getter
@Slf4j
public class SenderSupplyScenarioParametersAction extends PintAction {

  private final int documentCount;

  public SenderSupplyScenarioParametersAction(
      String platformPartyName,
      String carrierPartyName,
      PintAction previousAction,
      int documentCount) {
    super(
        carrierPartyName,
        platformPartyName,
        previousAction,
        "SupplyScenarioParameters[D:%d]".formatted(documentCount),
        -1);
    this.documentCount = documentCount;
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
    this.setDsp(this.getDsp().withDocumentCount(this.documentCount));
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return new SenderScenarioParameters(
      "TD reference",
      "WAVE",
      "-----BEGIN RSA PUBLIC KEY-----\n<YOUR PUBLIC SIGNING KEY HERE>\n-----END RSA PUBLIC KEY-----\n"
    ).toJson();
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Scenario details");
  }

}
