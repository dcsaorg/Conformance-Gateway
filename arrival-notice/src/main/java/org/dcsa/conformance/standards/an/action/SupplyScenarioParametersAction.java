package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.an.checks.ScenarioType;
import org.dcsa.conformance.standards.an.party.ANFilterParameter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class SupplyScenarioParametersAction extends ANAction {

  private final ScenarioType scenarioType;
  private final LinkedHashSet<ANFilterParameter> requiredParameters;
  private final LinkedHashSet<ANFilterParameter> optionalParameters;

  public SupplyScenarioParametersAction(
    String producerPartyName,
    ScenarioType scenarioType,
    ANFilterParameter[] requiredParameters,
    ANFilterParameter[] optionalParameters,
    String title) {
    super(producerPartyName, null, null, title);
    this.scenarioType = scenarioType;
    this.requiredParameters = new LinkedHashSet<>(Arrays.asList(requiredParameters));
    this.optionalParameters = new LinkedHashSet<>(Arrays.asList(optionalParameters));
    if (scenarioType != null) {
      this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
    }
  }

  @Override
  public void reset() {
    super.reset();
    if (scenarioType != null) {
      this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Provide values for the listed query parameters so the sandbox can retrieve at least one matching Arrival Notice. Optional parameters may be omitted.";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return examplePrompt();
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("exampleQueryParameters", examplePrompt());
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    JsonNode input = partyInput.path("input");
    if (!input.isObject()) {
      throw new UserFacingException("The input must contain a non-null object at 'input'.");
    }
    LinkedHashMap<String, String> supplied = new LinkedHashMap<>();
    input.properties().forEach(entry -> {
      ANFilterParameter parameter = ANFilterParameter.BY_QUERY_PARAM_NAME.get(entry.getKey());
      if (parameter == null) {
        throw new UserFacingException(
          "Unknown query parameter '%s'".formatted(entry.getKey()));
      }
      supplied.put(entry.getKey(), parameterValue(parameter, entry.getValue()));
    });
    for (ANFilterParameter parameter : requiredParameters) {
      if (!supplied.containsKey(parameter.getQueryParamName())) {
        throw new UserFacingException(
          "The required query parameter '%s' is missing".formatted(parameter.getQueryParamName()));
      }
    }
    this.getDspConsumer().accept(getDspSupplier().get().withSuppliedQueryParameters(supplied));
  }

  private static String parameterValue(ANFilterParameter parameter, JsonNode value) {
    String result;
    if (value.isArray()) {
      if (parameter == ANFilterParameter.LIMIT) {
        throw new UserFacingException("The value of 'limit' must be a single integer");
      }
      result = java.util.stream.StreamSupport.stream(value.spliterator(), false)
        .peek(element -> {
          if (!element.isTextual()) {
            throw new UserFacingException(
              "The value of '%s' must contain only strings when provided as an array"
                .formatted(parameter.getQueryParamName()));
          }
        })
        .map(JsonNode::asText)
        .collect(Collectors.joining(","));
    } else if (value.isValueNode()) {
      result = value.asText();
    } else {
      throw new UserFacingException(
        "The value of '%s' must be a scalar or an array of scalar values"
          .formatted(parameter.getQueryParamName()));
    }
    if (result.isBlank() || !result.equals(result.trim())) {
      throw new UserFacingException(
        "The value of '%s' must not be blank or surrounded by whitespace"
          .formatted(parameter.getQueryParamName()));
    }
    if (parameter == ANFilterParameter.LIMIT) {
      int parsedLimit;
      try {
        parsedLimit = Integer.parseInt(result);
      } catch (NumberFormatException e) {
        throw new UserFacingException("The value of 'limit' must be an integer");
      }
      if (parsedLimit < 1) {
        throw new UserFacingException("The value of 'limit' must be greater than or equal to 1");
      }
    }
    return result;
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  public Map<String, Boolean> getExpectedInputAttributes() {
    LinkedHashMap<String, Boolean> attributes = new LinkedHashMap<>();
    requiredParameters.forEach(parameter -> attributes.put(parameter.getQueryParamName(), true));
    optionalParameters.forEach(parameter -> attributes.put(parameter.getQueryParamName(), false));
    return attributes;
  }

  public ObjectNode examplePrompt() {
    ObjectNode prompt = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    getExpectedInputAttributes().keySet().forEach(name -> addExample(prompt, name));
    return prompt;
  }

  private static void addExample(ObjectNode prompt, String name) {
    switch (ANFilterParameter.BY_QUERY_PARAM_NAME.get(name)) {
      case TRANSPORT_DOCUMENT_REFERENCES ->
        prompt.set(name, JsonToolkit.OBJECT_MAPPER.createArrayNode().add("HHL71800000"));
      case EQUIPMENT_REFERENCES -> prompt.set(name, JsonToolkit.OBJECT_MAPPER.createArrayNode().add("HLCU1234567"));
      case PORT_OF_DISCHARGE -> prompt.put(name, "SGSIN");
      case VESSEL_IMO_NUMBER -> prompt.put(name, "9321483");
      case VESSEL_NAME -> prompt.put(name, "YM MASCULINITY");
      case CARRIER_IMPORT_VOYAGE_NUMBER -> prompt.put(name, "097E");
      case UNIVERSAL_IMPORT_VOYAGE_REFERENCE -> prompt.put(name, "2301W");
      case CARRIER_SERVICE_CODE -> prompt.put(name, "FE1");
      case UNIVERSAL_SERVICE_REFERENCE -> prompt.put(name, "SR12345A");
      case PORT_OF_DISCHARGE_ARRIVAL_DATE_MIN -> prompt.put(name, "2024-03-01");
      case PORT_OF_DISCHARGE_ARRIVAL_DATE_MAX -> prompt.put(name, "2024-03-31");
      case LIMIT -> prompt.put(name, 1);
    }
  }
}
