package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.an.party.ArrivalNoticeFilterParameter;
import org.dcsa.conformance.standards.an.party.SuppliedScenarioParameters;

import static org.dcsa.conformance.standards.an.party.ArrivalNoticeFilterParameter.TRANSPORT_DOCUMENT_REFERENCE;

@Getter
public class SupplyScenarioParametersAction extends ConformanceAction {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private final LinkedHashSet<ArrivalNoticeFilterParameter> arrivalNoticeFilterParameters;

  public SupplyScenarioParametersAction(
      String publisherPartyName, ArrivalNoticeFilterParameter... anFilterParameters) {
    super(
        publisherPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                Arrays.stream(anFilterParameters)
                    .map(ArrivalNoticeFilterParameter::getQueryParamName)
                    .collect(Collectors.joining(", "))));
    this.arrivalNoticeFilterParameters =
      Stream.of(anFilterParameters).collect(Collectors.toCollection(LinkedHashSet::new));
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
        arrivalNoticeFilterParameters.stream()
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        anFilterParameter ->
                            switch (anFilterParameter) {
                              case  TRANSPORT_DOCUMENT_REFERENCE-> "111111-333";
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
