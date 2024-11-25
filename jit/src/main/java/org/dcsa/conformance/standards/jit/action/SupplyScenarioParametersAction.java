package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.party.JitConsumer;
import org.dcsa.conformance.standards.jit.party.SuppliedScenarioParameters;

@Slf4j
public class SupplyScenarioParametersAction extends ConformanceAction {

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
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (suppliedScenarioParameters != null) {
      jsonState.set(PARAMETERS, suppliedScenarioParameters.toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    if (jsonState.has(PARAMETERS)) {
      suppliedScenarioParameters =
          SuppliedScenarioParameters.fromJson(jsonState.required(PARAMETERS));
    }
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
  }
}
