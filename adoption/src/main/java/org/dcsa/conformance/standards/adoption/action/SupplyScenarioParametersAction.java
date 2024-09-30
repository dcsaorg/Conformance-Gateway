package org.dcsa.conformance.standards.adoption.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.core.UserFacingException;
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
    this.filterParameters = new LinkedHashSet<>(Arrays.asList(filterParameters));
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
    try {
      JsonNode input = partyInput.get("input");

      // This should not be needed, but somehow it uses the SupplyScenarioParametersAction also on
      // DCSA role
      if (input instanceof NullNode) {
        return;
      }
      String interval = input.required("interval").asText();
      if (interval == null
          || !(interval.equals("day") || interval.equals("week") || interval.equals("month"))) {
        throw new UserFacingException("Invalid interval supplied: %s".formatted(interval));
      }
      String date = input.required("date").asText();
      DATE_FORMAT.parse(date);
      suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(input);
    } catch (IllegalArgumentException | DateTimeParseException e) {
      throw new UserFacingException("Invalid input: %s".formatted(e.getMessage()));
    }
  }
}
