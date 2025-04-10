package org.dcsa.conformance.standards.tnt.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.standards.tnt.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.tnt.party.TntFilterParameter;

@Getter
public class SupplyScenarioParametersAction extends TntAction {
  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private final Map<TntFilterParameter,String> tntFilterParameterMap;

  public SupplyScenarioParametersAction(
      String publisherPartyName, Map<TntFilterParameter, String> parameters) {
    super(
        publisherPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                parameters.keySet().stream()
                    .map(
                        parameter ->
                            parameter.getQueryParamName()
                                + (parameter.equals(TntFilterParameter.EVENT_TYPE)
                                    ? "=" + parameters.get(parameter) // the scenario title must be unique!
                                    : ""))
                    .collect(Collectors.joining(", "))),
        -1);
    this.tntFilterParameterMap = parameters;
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
    ArrayNode jsonTntFilterParameters = objectNode.putArray("tntFilterParametersQueryParam");
    tntFilterParameterMap.forEach((key, value) -> {
      ObjectNode parameterNode = jsonTntFilterParameters.addObject();
      parameterNode.put("parameter", key.getQueryParamName());
      parameterNode.put("value", value);
    });
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
        tntFilterParameterMap.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
      .toJson();
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    JsonNode inputNode = partyInput.get("input");
    Arrays.stream(TntFilterParameter.values())
        .map(TntFilterParameter::getQueryParamName)
        .filter(
            queryParamName ->
                queryParamName.startsWith(
                    TntFilterParameter.EVENT_CREATED_DATE_TIME.getQueryParamName()))
        .filter(inputNode::hasNonNull)
        .forEach(
            queryParamName -> {
              String dateValue = inputNode.path(queryParamName).asText();
              try {
                OffsetDateTime.parse(dateValue);
              } catch (DateTimeParseException e) {
                throw new UserFacingException(
                    "Invalid date-time format '%s' for input parameter '%s'"
                        .formatted(dateValue, queryParamName),
                    e);
              }
            });

    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(inputNode);
  }
}
