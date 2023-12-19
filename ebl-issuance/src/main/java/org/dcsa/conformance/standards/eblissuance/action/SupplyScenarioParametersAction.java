package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.eblissuance.party.SuppliedScenarioParameters;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SupplyScenarioParametersAction extends IssuanceAction {
  private SuppliedScenarioParameters suppliedScenarioParameters = null;

  public SupplyScenarioParametersAction(
      String sourcePartyName, String targetPartyName, ConformanceAction previousAction) {
    super(sourcePartyName, targetPartyName, previousAction, "Supply scenario parameters", -1);
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
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode cspNode = jsonState.get("suppliedScenarioParameters");
    if (cspNode != null) {
      suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(cspNode);
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Supply the parameters required by the scenario using the following format:";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return new SuppliedScenarioParameters("XMPL", "Example party code", "Example code list")
        .toJson();
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    super.handlePartyInput(partyInput);
    getSspConsumer().accept(SuppliedScenarioParameters.fromJson(partyInput.get("input")));
  }

  @Override
  protected Consumer<SuppliedScenarioParameters> getSspConsumer() {
    return csp -> this.suppliedScenarioParameters = csp;
  }

  @Override
  protected Supplier<SuppliedScenarioParameters> getSspSupplier() {
    return () -> suppliedScenarioParameters;
  }

  @Override
  protected Supplier<String> getTdrSupplier() {
    return null;
  }
}
