package org.dcsa.conformance.standards.cs.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import org.dcsa.conformance.standards.cs.model.CsDateUtils;
import org.dcsa.conformance.standards.cs.party.CsFilterParameter;
import org.dcsa.conformance.standards.cs.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends CsAction {

  private final LinkedHashSet<CsFilterParameter> csFilterParameters;
  private SuppliedScenarioParameters suppliedScenarioParameters = null;

  public SupplyScenarioParametersAction(
      String publisherPartyName, CsFilterParameter... csFilterParameters) {
    super(
        publisherPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                Arrays.stream(csFilterParameters)
                    .map(CsFilterParameter::getQueryParamName)
                    .collect(Collectors.joining(", "))),
        -1);
    this.csFilterParameters = new LinkedHashSet<>(Arrays.asList(csFilterParameters));
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
    ArrayNode jsonCsFilterParameters = objectNode.putArray("csFilterParametersQueryParamNames");
    csFilterParameters.forEach(
        csFilterParameter -> jsonCsFilterParameters.add(csFilterParameter.getQueryParamName()));
    return objectNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(null, "prompt-publisher-ssp.md");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return SuppliedScenarioParameters.fromMap(
            csFilterParameters.stream()
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        csFilterParameter ->
                            switch (csFilterParameter) {
                              case DATE, DEPARTURE_START_DATE, ARRIVAL_START_DATE ->
                                  CsDateUtils.getCurrentDate();
                              case DEPARTURE_END_DATE, ARRIVAL_END_DATE ->
                                  CsDateUtils.getEndDateAfter3Months();
                              default -> "TODO";
                            })))
        .toJson();
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    super.handlePartyInput(partyInput);
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
