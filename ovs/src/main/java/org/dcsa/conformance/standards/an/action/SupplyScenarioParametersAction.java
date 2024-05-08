package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.an.party.OvsFilterParameter;
import org.dcsa.conformance.standards.an.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends ConformanceAction {
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private final LinkedHashSet<OvsFilterParameter> ovsFilterParameters;

  public SupplyScenarioParametersAction(
      String publisherPartyName, OvsFilterParameter... ovsFilterParameters) {
    super(
        publisherPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                Arrays.stream(ovsFilterParameters)
                    .map(OvsFilterParameter::getQueryParamName)
                    .collect(Collectors.joining(", "))));
    this.ovsFilterParameters =
        Stream.of(ovsFilterParameters).collect(Collectors.toCollection(LinkedHashSet::new));
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
    if (jsonState.has("suppliedScenarioParameters")) {
      suppliedScenarioParameters =
          SuppliedScenarioParameters.fromJson(jsonState.required("suppliedScenarioParameters"));
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode objectNode = super.asJsonNode();
    ArrayNode jsonOvsFilterParameters = objectNode.putArray("ovsFilterParametersQueryParamNames");
    ovsFilterParameters.forEach(
        ovsFilterParameter -> jsonOvsFilterParameters.add(ovsFilterParameter.getQueryParamName()));
    return objectNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Use the following format to provide the values of the specified query parameters"
        + " for which your party can successfully process a GET request:";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return SuppliedScenarioParameters.fromMap(
            ovsFilterParameters.stream()
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        ovsFilterParameter ->
                            switch (ovsFilterParameter) {
                              case LIMIT -> "100";
                              case START_DATE, END_DATE -> DATE_FORMAT.format(new Date());
                              default -> "TODO";
                            })))
        .toJson();
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    super.handlePartyInput(partyInput);
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInput.get("input"));
  }
}
