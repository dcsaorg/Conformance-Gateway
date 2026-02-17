package org.dcsa.conformance.end.action;

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
  private static final String SUPPLIED_SCENARIO_PARAMETERS = "suppliedScenarioParameters";
  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String END_FILTER_PARAM_QUERY_PARAM_NAMES =
      "endorsementChainFilterParamQueryParamNames";

  public SupplyScenarioParametersAction(String providerPartyName, EndorsementChainFilterParameter...endorsementChainFilterParameters) {
    super(
        providerPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                Arrays.stream(endorsementChainFilterParameters)
                    .map(EndorsementChainFilterParameter::getParamName)
                    .collect(Collectors.joining(", "))));

    this.endorsementChainFilterParameters = new LinkedHashSet<>(Arrays.asList(endorsementChainFilterParameters));

  }
  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (suppliedScenarioParameters != null) {
      jsonState.set(SUPPLIED_SCENARIO_PARAMETERS, suppliedScenarioParameters.toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    if (jsonState.has(SUPPLIED_SCENARIO_PARAMETERS)) {
      suppliedScenarioParameters =
          SuppliedScenarioParameters.fromJson(jsonState.required(SUPPLIED_SCENARIO_PARAMETERS));
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode objectNode = super.asJsonNode();
    ArrayNode jsonEndorsementChainFilterParam =
        objectNode.putArray(END_FILTER_PARAM_QUERY_PARAM_NAMES);
    endorsementChainFilterParameters.forEach(
        endorsementChainFilterParameter ->
            jsonEndorsementChainFilterParam.add(endorsementChainFilterParameter.getParamName()));
    return objectNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        null,
        "prompt-provider-ssp.md");
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
                              case TRANSPORT_DOCUMENT_REFERENCE -> "HHL71800000";
                              case TRANSPORT_DOCUMENT_SUB_REFERENCE ->
                                  "fc5009a7-25ad-4bb0-9892-4e2dea6bcdd9";
                              case CARRIER_SCAC_CODE -> "MAEU";
                              default -> "TODO";
                            })))
        .toJson();
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInput.get("input"));
    this.getDspConsumer()
        .accept(
            getDspSupplier()
                .get()
                .withTransportDocumentReference(
                    partyInput.get("input").get(TRANSPORT_DOCUMENT_REFERENCE).asText()));
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
