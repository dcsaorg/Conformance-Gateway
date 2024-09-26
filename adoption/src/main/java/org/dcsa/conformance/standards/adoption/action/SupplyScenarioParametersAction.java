package org.dcsa.conformance.standards.adoption.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.adoption.party.FilterParameter;
import org.dcsa.conformance.standards.adoption.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends ConformanceAction {
  public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private final LinkedHashSet<FilterParameter> filterParameters;

  public SupplyScenarioParametersAction(
      String publisherPartyName, FilterParameter... filterParameters) {
    super(
        publisherPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                Arrays.stream(filterParameters)
                    .map(FilterParameter::getQueryParamName)
                    .collect(Collectors.joining(", "))));
    this.filterParameters =
        Stream.of(filterParameters).collect(Collectors.toCollection(LinkedHashSet::new));
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
    ArrayNode jsonFilterParameters = objectNode.putArray("FilterParametersQueryParamNames");
    filterParameters.forEach(
        filterParameter -> jsonFilterParameters.add(filterParameter.getQueryParamName()));
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
            filterParameters.stream()
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        filterParameter ->
                            switch (filterParameter) {
                              case INTERVAL -> "day";
                              case DATE -> LocalDateTime.now().format(DATE_FORMAT);
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