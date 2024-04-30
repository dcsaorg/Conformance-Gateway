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
import org.dcsa.conformance.standards.an.party.ArrivalNoticeFilterParameter;
import org.dcsa.conformance.standards.an.party.OvsFilterParameter;
import org.dcsa.conformance.standards.an.party.SuppliedScenarioParameters;

import static org.dcsa.conformance.standards.an.party.ArrivalNoticeFilterParameter.LIMIT;

@Getter
public class SupplyScenarioParametersAction extends ConformanceAction {
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private SuppliedScenarioParameters suppliedScenarioParameters = null;

  public SupplyScenarioParametersAction(
      String publisherPartyName, ArrivalNoticeFilterParameter... ovsFilterParameters) {
    super(
        publisherPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                Arrays.stream(ovsFilterParameters)
                    .map(ArrivalNoticeFilterParameter::getQueryParamName)
                    .collect(Collectors.joining(", "))));
  }

  @Override
  public void reset() {
    super.reset();
    suppliedScenarioParameters = null;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode objectNode = super.asJsonNode();
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
