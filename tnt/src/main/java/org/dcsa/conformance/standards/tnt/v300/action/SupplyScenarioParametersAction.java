package org.dcsa.conformance.standards.tnt.v300.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.tnt.v300.party.TntQueryParameters;
import org.dcsa.conformance.standards.tnt.v300.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.tnt.v300.party.TntConstants;

@Getter
public class SupplyScenarioParametersAction extends TntAction {

  private final Set<TntQueryParameters> tntQueryParameters;
  private SuppliedScenarioParameters suppliedScenarioParameters = null;

  public SupplyScenarioParametersAction(
      String sourcePartyName, TntQueryParameters... queryParameters) {
    super(
        sourcePartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                Arrays.stream(queryParameters)
                    .map(TntQueryParameters::name)
                    .collect(Collectors.joining(", "))));
    this.tntQueryParameters = new LinkedHashSet<>(Arrays.asList(queryParameters));
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (suppliedScenarioParameters != null) {
      jsonState.set(TntConstants.SUPPLIED_SCENARIO_PARAMETERS, suppliedScenarioParameters.toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    if (jsonState.has(TntConstants.SUPPLIED_SCENARIO_PARAMETERS)) {
      suppliedScenarioParameters =
          SuppliedScenarioParameters.fromJson(jsonState.required(TntConstants.SUPPLIED_SCENARIO_PARAMETERS));
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode objectNode = super.asJsonNode();
    ArrayNode jsonTntQueryParameters = objectNode.putArray(TntConstants.TNT_QUERY_PARAMETERS);
    tntQueryParameters.forEach(
        tntQueryParameter -> jsonTntQueryParameters.add(tntQueryParameter.getParameterName()));
    return objectNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    Set<TntQueryParameters> parametersToDisplay =
        tntQueryParameters.isEmpty()
            ? new LinkedHashSet<>(Arrays.asList(TntQueryParameters.values()))
            : tntQueryParameters;

    String parametersList =
        parametersToDisplay.stream()
            .map(param -> "  - " + param.getParameterName() + " (" + param.name() + ")")
            .collect(Collectors.joining(System.lineSeparator()));

    if (tntQueryParameters.isEmpty()) {
      return "Specify any combination of query parameters that the sandbox can use in a GET request to fetch events from your system."
          + " The synthetic Event Consumer running in the sandbox will send a GET request built using the query parameters you provide."
          + "%n%nThe available query parameters are:%n%s".formatted(parametersList);
    }

    // Create readable parameter names list for the prompt
    String parameterNames =
        tntQueryParameters.stream()
            .map(param -> "'" + param.getParameterName() + "'")
            .collect(Collectors.joining(" and "));

    // Create parameter codes list for the action title example
    String parameterCodes =
        tntQueryParameters.stream().map(TntQueryParameters::name).collect(Collectors.joining(", "));

    return "Using the example format below, provide the query parameter filter(s) specified in the action title"
        + " that the sandbox can use in a GET request to fetch events from your system."
        + " The action 'Supply scenario parameters (%s)' requires you to provide values for the %s query parameter %s."
            .formatted(parameterCodes, parameterNames, tntQueryParameters.size() > 1 ? "s" : "")
        + "%n%nThe required query parameters are:%n%s".formatted(parametersList);
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return examplePrompt(this.tntQueryParameters);
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInput.get(TntConstants.INPUT));
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

  public static ObjectNode examplePrompt(Set<TntQueryParameters> tntQueryParameters) {
    ObjectNode promptNode = JsonToolkit.OBJECT_MAPPER.createObjectNode();

    for (TntQueryParameters param : tntQueryParameters) {
      String exampleValue =
          switch (param) {
            case CBR -> "ABC709951";
            case TDR -> "HHL71800000";
            case ER -> "APZU4812090";
            case ET -> "EQUIPMENT,IOT,REEFER";
            case E_UDT_MIN -> "2025-01-23T01:23:45Z";
            case E_UDT_MAX -> "2025-02-23T01:23:45Z";
            case LIMIT -> "5";
            case CURSOR -> "ExampleNextPageCursor";
          };
      promptNode.put(param.getParameterName(), exampleValue);
    }

    return promptNode;
  }
}
