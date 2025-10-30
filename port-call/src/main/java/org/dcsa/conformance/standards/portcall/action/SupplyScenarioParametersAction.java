package org.dcsa.conformance.standards.portcall.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.standards.portcall.JitScenarioContext;
import org.dcsa.conformance.standards.portcall.model.JitServiceTypeSelector;
import org.dcsa.conformance.standards.portcall.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.portcall.party.JitConsumer;
import org.dcsa.conformance.standards.portcall.party.SuppliedScenarioParameters;

@Slf4j
public class SupplyScenarioParametersAction extends PortCallAction {

  @Getter private SuppliedScenarioParameters suppliedScenarioParameters;
  private final JitServiceTypeSelector selector;
  private final boolean isFYI;

  public SupplyScenarioParametersAction(
      JitScenarioContext context, JitServiceTypeSelector selector, boolean isFYI) {
    super(context.consumerPartyName(), null, null, calculateTitle(selector, isFYI));
    this.selector = selector;
    this.isFYI = isFYI;
  }

  // Adjusting the title, to make the scenarios in the whole JIT list unique
  private static String calculateTitle(JitServiceTypeSelector selector, boolean isFYI) {
    if (selector == JitServiceTypeSelector.GIVEN) {
      String fyi = isFYI ? "(FYI)" : "";
      return "SupplyScenarioParameters%s".formatted(fyi);
    }
    String fyi = isFYI ? ", FYI" : "";
    return "SupplyScenarioParameters(%s%s)".formatted(selector.getFullName(), fyi);
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
    return getMarkdownFile("prompt-csp.md");
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
  protected void doHandlePartyInput(JsonNode partyInput) {
    log.info("SupplyScenarioParametersAction.handlePartyInput({})", partyInput.toPrettyString());
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInput.get("input"));

    dsp =
        new DynamicScenarioParameters(
            null,
            null,
            suppliedScenarioParameters.serviceTypeCode(),
            suppliedScenarioParameters.portCallID(),
            suppliedScenarioParameters.terminalCallID(),
            suppliedScenarioParameters.portCallServiceID(),
            selector,
            isFYI);
  }
}
