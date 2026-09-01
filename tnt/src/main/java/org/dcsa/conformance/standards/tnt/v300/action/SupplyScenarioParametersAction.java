package org.dcsa.conformance.standards.tnt.v300.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.tnt.v300.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.tnt.v300.party.TntConstants;
import org.dcsa.conformance.standards.tnt.v300.party.TntQueryParameters;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class SupplyScenarioParametersAction extends TntAction {

  private final Set<TntQueryParameters> requiredQueryParameters;
  private final Set<TntQueryParameters> optionalQueryParameters;
  private SuppliedScenarioParameters suppliedScenarioParameters = null;

  public SupplyScenarioParametersAction(
    String sourcePartyName, TntQueryParameters... queryParameters) {
    this(sourcePartyName, new LinkedHashSet<>(Arrays.asList(queryParameters)), Collections.emptySet());
  }

  private SupplyScenarioParametersAction(
    String sourcePartyName,
    Set<TntQueryParameters> requiredQueryParameters,
    Set<TntQueryParameters> optionalQueryParameters) {
    super(
      sourcePartyName,
      null,
      null,
      "SupplyScenarioParameters(%s)"
        .formatted(
          formatActionTitle(requiredQueryParameters, optionalQueryParameters)));
    this.requiredQueryParameters = Set.copyOf(requiredQueryParameters);
    this.optionalQueryParameters = Set.copyOf(optionalQueryParameters);
  }

  public static SupplyScenarioParametersAction optional(String sourcePartyName, TntQueryParameters... optionalQueryParameters) {
    return new SupplyScenarioParametersAction(
      sourcePartyName,
      Collections.emptySet(),
      new LinkedHashSet<>(Arrays.asList(optionalQueryParameters)));
  }

  private static String formatActionTitle(Set<TntQueryParameters> requiredQueryParameters, Set<TntQueryParameters> optionalQueryParameters) {
    String requiredCodes = requiredQueryParameters.stream()
      .map(TntQueryParameters::name)
      .collect(Collectors.joining(", "));
    String optionalCodes = optionalQueryParameters.stream()
      .map(TntQueryParameters::name)
      .collect(Collectors.joining(", "));

    if (requiredCodes.isEmpty()) {
      return "optional: " + optionalCodes;
    }
    if (optionalCodes.isEmpty()) {
      return requiredCodes;
    }
    return requiredCodes + " | optional: " + optionalCodes;
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
    Set<TntQueryParameters> allQueryParameters = getAllQueryParameters();

    ArrayNode jsonTntQueryParameters = objectNode.putArray(TntConstants.TNT_QUERY_PARAMETERS);
    allQueryParameters.forEach(tntQueryParameter -> jsonTntQueryParameters.add(tntQueryParameter.getParameterName()));

    ArrayNode required = objectNode.putArray(TntConstants.REQUIRED_TNT_QUERY_PARAMETERS);
    requiredQueryParameters.forEach(param -> required.add(param.getParameterName()));

    ArrayNode optional = objectNode.putArray(TntConstants.OPTIONAL_TNT_QUERY_PARAMETERS);
    optionalQueryParameters.forEach(param -> optional.add(param.getParameterName()));

    return objectNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    Map<String, String> replacements =
      Map.of(
        "REQUIRED_PARAMETERS_PLACEHOLDER", formatParameterList(requiredQueryParameters),
        "OPTIONAL_PARAMETERS_PLACEHOLDER", formatParameterList(optionalQueryParameters));
    if (optionalQueryParameters.isEmpty()) {
      return getMarkdownHumanReadablePrompt(replacements, "prompt-producer-supply-scenario-parameters.md");
    }
    return getMarkdownHumanReadablePrompt(replacements, "prompt-producer-supply-scenario-parameters-optional.md");
  }

  @Override
  public Map<String, Boolean> getExpectedInputAttributes() {
    LinkedHashMap<String, Boolean> expectedAttributes = new LinkedHashMap<>();
    requiredQueryParameters.forEach(parameter -> expectedAttributes.put(parameter.getParameterName(), true));
    optionalQueryParameters.forEach(parameter -> expectedAttributes.put(parameter.getParameterName(), false));
    return expectedAttributes;
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return examplePrompt(getAllQueryParameters());
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    JsonNode inputNode = partyInput.get(TntConstants.INPUT);
    if (inputNode == null || inputNode.isNull()) {
      inputNode = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    }
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(inputNode);
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

  private Set<TntQueryParameters> getAllQueryParameters() {
    if (requiredQueryParameters.isEmpty() && optionalQueryParameters.isEmpty()) {
      return new LinkedHashSet<>(Arrays.asList(TntQueryParameters.values()));
    }
    Set<TntQueryParameters> all = new LinkedHashSet<>(requiredQueryParameters);
    all.addAll(optionalQueryParameters);
    return all;
  }

  private String formatParameterList(Set<TntQueryParameters> parameters) {
    return parameters.stream()
      .map(TntQueryParameters::getParameterName)
      .map(parameterName -> "- `" + parameterName + "`")
      .collect(Collectors.joining(System.lineSeparator()));
  }
}
