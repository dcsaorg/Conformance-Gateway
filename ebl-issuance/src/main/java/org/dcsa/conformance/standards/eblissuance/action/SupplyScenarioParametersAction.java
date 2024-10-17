package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.dcsa.conformance.standards.eblissuance.party.SuppliedScenarioParameters;

public class SupplyScenarioParametersAction extends IssuanceAction {
  private final EblType eblType;
  private final IssuanceResponseCode responseCode;
  private SuppliedScenarioParameters suppliedScenarioParameters = null;


  public SupplyScenarioParametersAction(
      String sourcePartyName, String targetPartyName, IssuanceAction previousAction, EblType eblType, IssuanceResponseCode code) {
    super(sourcePartyName, targetPartyName, previousAction, "Supply scenario parameters [%s]".formatted(eblType.name()), -1);
    this.eblType = eblType;
    this.responseCode = code;
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
    return (switch (eblType) {
      case BLANK_EBL -> new SuppliedScenarioParameters(
        "BOLE (platform code)",
        "Legal name of issueTo party",
        "Party code of issueTo party",
        "Bolero (code list name for issueTo party)",
        null,
        null,
        null
      );
      default -> new SuppliedScenarioParameters(
        "BOLE (platform code)",
        "Legal name of issue to party",
        "Party code of issue to party",
        "Bolero (code list name for issue to party)",
        "Legal name of consignee/endorsee",
        "Party code of consignee/endorsee",
        "Bolero (code list name for consignee/endorsee)"
      );
    }).toJson();
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    super.handlePartyInput(partyInput);
    setDsp(getDsp().withEblType(eblType));
    getSspConsumer().accept(SuppliedScenarioParameters.fromJson(partyInput.get("input")));
  }

  @Override
  public ObjectNode asJsonNode() {
      return super.asJsonNode()
      .put("eblType", eblType.name()).put("responseCode",responseCode.standardCode);
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
