package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.jit.party.JitFilterParameter;
import org.dcsa.conformance.standards.jit.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends ConformanceAction {

  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private final LinkedHashSet<JitFilterParameter> jitFilterParameters;

  public SupplyScenarioParametersAction(
      String publisherPartyName, JitFilterParameter... jitFilterParameters) {
    super(
        publisherPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                Arrays.stream(jitFilterParameters)
                    .map(JitFilterParameter::getQueryParamName)
                    .collect(Collectors.joining(", "))));
    this.jitFilterParameters = new LinkedHashSet<>(Arrays.asList(jitFilterParameters));
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
    ArrayNode jsonJitFilterParameters = objectNode.putArray("jitFilterParametersQueryParamNames");
    jitFilterParameters.forEach(
        jitFilterParameter -> jsonJitFilterParameters.add(jitFilterParameter.getQueryParamName()));
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
            jitFilterParameters.stream()
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        jitFilterParameter ->
                            switch (jitFilterParameter) {
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
