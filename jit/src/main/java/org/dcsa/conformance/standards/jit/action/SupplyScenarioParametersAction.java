package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector;
import org.dcsa.conformance.standards.jit.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.jit.party.JitConsumer;
import org.dcsa.conformance.standards.jit.party.SuppliedScenarioParameters;

@Slf4j
public class SupplyScenarioParametersAction extends JitAction {

  @Getter private SuppliedScenarioParameters suppliedScenarioParameters;
  private final JitServiceTypeSelector selector;
  private final boolean isFYI;

  public SupplyScenarioParametersAction(
    JitScenarioContext context, JitServiceTypeSelector selector, boolean isFYI) {
    super(context.consumerPartyName(), null, null, "SupplyScenarioParameters");
    this.selector = selector;
    this.isFYI = isFYI;
  }

  @Override
  public void reset() {
    super.reset();
    suppliedScenarioParameters = null;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("selector", selector.name());
    return jsonNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Supply the parameters required by the scenario:";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return JitConsumer.createSuppliedScenarioParameters(selector).toJson();
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
            suppliedScenarioParameters.serviceType(),
            suppliedScenarioParameters.portCallID(),
            suppliedScenarioParameters.terminalCallID(),
            suppliedScenarioParameters.portCallServiceID(),
            selector,
            isFYI);
  }
}
