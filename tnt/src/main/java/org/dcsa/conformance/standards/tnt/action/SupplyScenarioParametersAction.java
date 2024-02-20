package org.dcsa.conformance.standards.tnt.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.tnt.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.tnt.party.TntFilterParameter;

@Getter
public class SupplyScenarioParametersAction extends ConformanceAction {
  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private final LinkedHashSet<TntFilterParameter> TntFilterParameters;

  public SupplyScenarioParametersAction(
      String publisherPartyName, TntFilterParameter... TntFilterParameters) {
    super(
        publisherPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                Arrays.stream(TntFilterParameters)
                    .map(TntFilterParameter::getQueryParamName)
                    .collect(Collectors.joining(", "))));
    this.TntFilterParameters =
        Stream.of(TntFilterParameters).collect(Collectors.toCollection(LinkedHashSet::new));
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
    ArrayNode jsonTntFilterParameters = objectNode.putArray("tntFilterParametersQueryParamNames");
    TntFilterParameters.forEach(
        TntFilterParameter -> jsonTntFilterParameters.add(TntFilterParameter.getQueryParamName()));
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
            TntFilterParameters.stream()
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        TntFilterParameter ->
                            switch (TntFilterParameter) {
                              case LIMIT -> "100";
                              case EVENT_CREATED_DATE_TIME,
                                      EVENT_CREATED_DATE_TIME_EQ,
                                      EVENT_CREATED_DATE_TIME_GT,
                                      EVENT_CREATED_DATE_TIME_GTE,
                                      EVENT_CREATED_DATE_TIME_LT,
                                      EVENT_CREATED_DATE_TIME_LTE ->
                                  ZonedDateTime.now()
                                      .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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
