package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.dcsa.conformance.standards.eblissuance.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceCarrier;
import org.dcsa.conformance.standards.eblissuance.party.SuppliedScenarioParameters;

public class PlatformScenarioParametersAction extends IssuanceAction {

  private SuppliedScenarioParameters suppliedScenarioParameters = null;

  public PlatformScenarioParametersAction(
      String sourcePartyName,
      String targetPartyName,
      IssuanceAction previousAction) {
    super(sourcePartyName, targetPartyName, previousAction, "SupplyCSP [Document Parties]", -1);
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
    return getMarkdownHumanReadablePrompt(
        Map.of(
            "PUBLIC_KEY",
            EblIssuanceCarrier.getCarrierPublicKey(),
            "KEY_ID",
            EblIssuanceCarrier.getCarrierKeyId()),
        "prompt-platform-psp.md");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return new SuppliedScenarioParameters(
                "DCSA",
                "Legal name of issue to party",
                "Code list provider of issue to party",
                "Party code of issue to party",
                "DCSA (code list name for issue to party)",
                "Legal name of shipper",
                "Code list provider of shipper",
                "Party code of shipper",
                "DCSA (code list name for shipper)",
                "Legal name of consignee",
                "Code list provider of consignee",
                "Party code of consignee",
                "DCSA (code list name for consignee)",
                "Legal name of issuing party",
                "Code list provider of issuing party",
                "Party code of issuing party",
                "DCSA (code list name for issuing party)")
        .toJson();
  }

  @Override
  public Map<String, Boolean> getExpectedInputAttributes() {
    Map<String, Boolean> expectedAttributes = super.getExpectedInputAttributes();
    expectedAttributes.put("issueToCodeListName", false);
    expectedAttributes.put("shipperLegalName", false);
    expectedAttributes.put("shipperCodeListProvider", false);
    expectedAttributes.put("shipperPartyCode", false);
    expectedAttributes.put("shipperCodeListName", false);
    expectedAttributes.put("consigneeOrEndorseeLegalName", false);
    expectedAttributes.put("consigneeOrEndorseeCodeListProvider", false);
    expectedAttributes.put("consigneeOrEndorseePartyCode", false);
    expectedAttributes.put("consigneeOrEndorseeCodeListName", false);
    expectedAttributes.put("issuingPartyLegalName", false);
    expectedAttributes.put("issuingPartyCodeListProvider", false);
    expectedAttributes.put("issuingPartyPartyCode", false);
    expectedAttributes.put("issuingPartyCodeListName", false);
    return expectedAttributes;
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
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
  protected Supplier<CarrierScenarioParameters> getCspSupplier() {
    return () -> new CarrierScenarioParameters(EblIssuanceCarrier.getCarrierPublicKey());
  }

  @Override
  protected Supplier<String> getTdrSupplier() {
    return null;
  }
}
