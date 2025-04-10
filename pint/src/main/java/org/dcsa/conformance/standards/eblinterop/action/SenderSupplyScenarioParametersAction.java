package org.dcsa.conformance.standards.eblinterop.action;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.standards.eblinterop.models.SenderScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.party.PintSendingPlatform;

@Getter
@Slf4j
public class SenderSupplyScenarioParametersAction extends PintAction {
  public static final String ACTION_PREFIX = "SupplyScenarioParameters";
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
        "%s[D:%d]".formatted(ACTION_PREFIX, documentCount),
        -1);
    this.documentCount = documentCount;
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    var ssp = SenderScenarioParameters.fromJson(partyInput.path("input"));
    ssp.validate();
    this.setSsp(ssp);
    this.setDsp(this.getDsp().withDocumentCount(this.documentCount));
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return PintSendingPlatform.sendingScenarioParameters().toJson();
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Please provide these scenario details. Additional documents required: %d".formatted(documentCount));
  }

}
