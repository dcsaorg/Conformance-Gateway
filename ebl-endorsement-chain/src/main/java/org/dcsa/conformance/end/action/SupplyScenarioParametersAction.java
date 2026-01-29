package org.dcsa.conformance.end.action;

import static org.dcsa.conformance.end.party.EndorsementChainFilterParameter.CARRIER_SCAC_CODE;
import static org.dcsa.conformance.end.party.EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_REFERENCE;
import static org.dcsa.conformance.end.party.EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_SUB_REFERENCE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.end.party.EndorsementChainFilterParameter;
import org.dcsa.conformance.end.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends EndorsementChainAction{

  private final LinkedHashSet<EndorsementChainFilterParameter> endorsementChainFilterParameters;
  private SuppliedScenarioParameters suppliedScenarioParameters = null;

  public SupplyScenarioParametersAction(String providerPartyName, EndorsementChainFilterParameter...endorsementChainFilterParameters) {
    super(
      providerPartyName,
      null,
      null,
      "SupplyScenarioParameters(%s)"
        .formatted(
          Arrays.stream(endorsementChainFilterParameters)
            .map(EndorsementChainFilterParameter::getQueryParamName)
            .collect(Collectors.joining(", "))));

    this.endorsementChainFilterParameters = new LinkedHashSet<>(Arrays.asList(endorsementChainFilterParameters));
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
    if (jsonState.has("suppliedScenarioParameters")) {
      suppliedScenarioParameters =
        SuppliedScenarioParameters.fromJson(jsonState.required("suppliedScenarioParameters"));
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode objectNode = super.asJsonNode();
    ArrayNode jsonEndorsementChainFilterParam = objectNode.putArray("endorsementChainFilterParamQueryParamNames");
    endorsementChainFilterParameters.forEach(
      endorsementChainFilterParameter -> jsonEndorsementChainFilterParam.add(endorsementChainFilterParameter.getQueryParamName()));
    return objectNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(null, "prompt-publisher-ssp.md");
  }



  @Override
  public JsonNode getJsonForHumanReadablePrompt() {

    return SuppliedScenarioParameters.fromMap(
        endorsementChainFilterParameters.stream()
          .collect(
            Collectors.toMap(
              Function.identity(),
              endorsementChainFilterParameter ->
                switch (endorsementChainFilterParameter) {
                  case TRANSPORT_DOCUMENT_REFERENCE -> "";
                  case TRANSPORT_DOCUMENT_SUB_REFERENCE -> "";
                  case CARRIER_SCAC_CODE -> "";
                  default -> "TODO";
                })))
      .toJson();
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInput.get("input"));
  }

  @Override
  public void reset() {
    super.reset();
    suppliedScenarioParameters = null;
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }
}
