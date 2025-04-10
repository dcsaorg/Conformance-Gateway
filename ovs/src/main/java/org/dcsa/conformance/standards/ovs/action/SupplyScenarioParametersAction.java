package org.dcsa.conformance.standards.ovs.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.standards.ovs.party.OvsFilterParameter;
import org.dcsa.conformance.standards.ovs.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends OvsAction {
  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private final Map<OvsFilterParameter, String> ovsFilterParameterMap;

  public SupplyScenarioParametersAction(
      String publisherPartyName, Map<OvsFilterParameter, String> parameters) {
    super(
        publisherPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                parameters.entrySet().stream()
                    .map(entry -> entry.getKey().getQueryParamName() + "=" + entry.getValue())
                    .collect(Collectors.joining(", "))),
        -1);
    this.ovsFilterParameterMap = parameters;
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
    ArrayNode jsonOvsFilterParameters = objectNode.putArray("ovsFilterParametersQueryParam");
    ovsFilterParameterMap.forEach(
        (key, value) -> {
          ObjectNode parameterNode = jsonOvsFilterParameters.addObject();
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
            ovsFilterParameterMap.entrySet().stream()
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
    Arrays.stream(OvsFilterParameter.values())
        .map(OvsFilterParameter::getQueryParamName)
        .filter(
            queryParamName ->
                queryParamName.startsWith(
                    OvsFilterParameter.START_DATE.getQueryParamName())
              || queryParamName.startsWith(OvsFilterParameter.END_DATE.getQueryParamName()))
        .filter(inputNode::hasNonNull)
        .forEach(
            queryParamName -> {
              String dateValue = inputNode.path(queryParamName).asText();
              try {
                LocalDate.parse(dateValue, DateTimeFormatter.ISO_DATE);
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
