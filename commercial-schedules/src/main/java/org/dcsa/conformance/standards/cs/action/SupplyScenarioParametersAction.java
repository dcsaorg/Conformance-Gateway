package org.dcsa.conformance.standards.cs.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.cs.party.CsFilterParameter;
import org.dcsa.conformance.standards.cs.party.SuppliedScenarioParameters;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@Getter
public class SupplyScenarioParametersAction extends ConformanceAction {

  private final LinkedHashSet<CsFilterParameter> csFilterParameters;
  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  public SupplyScenarioParametersAction(
    String publisherPartyName, CsFilterParameter... csFilterParameters) {
    super(
      publisherPartyName,
      null,
      null,
      "SupplyScenarioParameters(%s)"
        .formatted(
          Arrays.stream(csFilterParameters)
            .map(CsFilterParameter::getQueryParamName)
            .collect(Collectors.joining(", "))));
    this.csFilterParameters =
      Stream.of(csFilterParameters).collect(Collectors.toCollection(LinkedHashSet::new));
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
    ArrayNode jsonOvsFilterParameters = objectNode.putArray("csFilterParametersQueryParamNames");
    csFilterParameters.forEach(
      csFilterParameter -> jsonOvsFilterParameters.add(csFilterParameter.getQueryParamName()));
    return objectNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return null;
  }
}
