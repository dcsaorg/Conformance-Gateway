package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.Getter;
import org.dcsa.conformance.standards.eblissuance.party.SuppliedScenarioParameters;

public class PlatformScenarioParametersAction extends IssuanceAction {
  private final EblType eblType;
  @Getter
  private IssuanceResponseCode responseCode;
  private SuppliedScenarioParameters suppliedScenarioParameters = null;

  public PlatformScenarioParametersAction(
      String sourcePartyName,
      String targetPartyName,
      IssuanceAction previousAction,
      EblType eblType,
      IssuanceResponseCode code) {
    super(
        sourcePartyName,
        targetPartyName,
        previousAction,
        "Platform scenario parameters [%s %s]"
            .formatted(eblType.name(), code.standardCode),
        -1);
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
    if (responseCode != null) {
      jsonState.put("responseCode", responseCode.toString());
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
    if (jsonState.get("responseCode") != null) {
      responseCode = IssuanceResponseCode.valueOf(jsonState.required("responseCode").asText());
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Supply the parameters that the synthetic carrier should use when constructing an issuance request, such that when your platform system receives the issuance request, it sends back an asynchronous response with the code "
        + responseCode.standardCode;
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    String sendToPlatform = responseCode.sendToPlatform;
    return (Objects.equals(eblType, EblType.BLANK_EBL)
            ? new SuppliedScenarioParameters(
                sendToPlatform,
                "Legal name of issueTo party",
                "Party code of issueTo party",
                "DCSA (code list name for issueTo party)",
                null,
                null,
                null)
            : new SuppliedScenarioParameters(
                sendToPlatform,
                "Legal name of issue to party",
                "Party code of issue to party",
                "DCSA (code list name for issue to party)",
                "Legal name of consignee/endorsee",
                "Party code of consignee/endorsee",
                "DCSA (code list name for consignee/endorsee)"))
        .toJson();
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
        .put("eblType", eblType.name())
        .put("responseCode", responseCode.standardCode);
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
