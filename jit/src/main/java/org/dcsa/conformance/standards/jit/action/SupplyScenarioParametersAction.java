package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.jit.party.JitConsumer;
import org.dcsa.conformance.standards.jit.party.SuppliedScenarioParameters;

@Slf4j
public class SupplyScenarioParametersAction extends JitAction {

  public static final String PARAMETERS = "suppliedScenarioParameters";
  @Getter private SuppliedScenarioParameters suppliedScenarioParameters;

  public SupplyScenarioParametersAction(JitScenarioContext context) {
    super(context.consumerPartyName(), null, null, "SupplyScenarioParameters");
  }

  @Override
  public void reset() {
    super.reset();
    suppliedScenarioParameters = null;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Supply the parameters required by the scenario:";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return JitConsumer.createSuppliedScenarioParameters().toJson();
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    log.info("SupplyScenarioParametersAction.handlePartyInput({})", partyInput.toPrettyString());
    super.handlePartyInput(partyInput);
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInput.get("input"));

    dsp =
      new DynamicScenarioParameters(
        null,
        null,
        null,
        null,
        null,
        suppliedScenarioParameters.portCallID(),
        null,
        suppliedScenarioParameters.portCallServiceID());
  }
}
