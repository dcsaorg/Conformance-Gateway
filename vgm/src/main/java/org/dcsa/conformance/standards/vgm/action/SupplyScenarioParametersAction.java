package org.dcsa.conformance.standards.vgm.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.vgm.checks.VgmQueryParameters;
import org.dcsa.conformance.standards.vgm.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends VgmAction {

  private static final String SUPPLIED_SCENARIO_PARAMETERS = "suppliedScenarioParameters";

  private final Set<VgmQueryParameters> vgmQueryParameters;
  private SuppliedScenarioParameters suppliedScenarioParameters = null;

  public SupplyScenarioParametersAction(
      String sourcePartyName, VgmQueryParameters... queryParameters) {
    super(
        sourcePartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)"
            .formatted(
                Arrays.stream(queryParameters)
                    .map(VgmQueryParameters::name)
                    .collect(Collectors.joining(", "))));
    this.vgmQueryParameters = new LinkedHashSet<>(Arrays.asList(queryParameters));
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (suppliedScenarioParameters != null) {
      jsonState.set(SUPPLIED_SCENARIO_PARAMETERS, suppliedScenarioParameters.toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    if (jsonState.has(SUPPLIED_SCENARIO_PARAMETERS)) {
      suppliedScenarioParameters =
          SuppliedScenarioParameters.fromJson(jsonState.required(SUPPLIED_SCENARIO_PARAMETERS));
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode objectNode = super.asJsonNode();
    ArrayNode jsonVgmQueryParameters = objectNode.putArray("vgmQueryParameters");
    vgmQueryParameters.forEach(
        vgmQueryParameter -> jsonVgmQueryParameters.add(vgmQueryParameter.getParameterName()));
    return objectNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Using the example format below, provide the query parameter filter(s) specified in the action title"
        + " that the sandbox can use in a GET request to fetch VGM declarations from your system."
        + " For example, if the action is 'Supply parameters (CBR+ER)', provide values for 'carrierBookingReference'"
        + " and 'equipmentReference' query parameters.";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return examplePrompt(this.vgmQueryParameters);
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInput.get("input"));
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

  public static ObjectNode examplePrompt(Set<VgmQueryParameters> vgmQueryParameters) {
    ObjectNode promptNode = JsonToolkit.OBJECT_MAPPER.createObjectNode();

    for (VgmQueryParameters param : vgmQueryParameters) {
      String exampleValue =
          switch (param) {
            case CBR -> "ABC709951";
            case ER -> "APZU4812090";
            case TDR -> "HHL71800000";
            case DDT_MIN -> "2025-01-23T01:23:45Z";
            case DDT_MAX -> "2025-02-23T01:23:45Z";
            case LIMIT -> "10";
            case CURSOR -> "ExampleNextPageCursor";
          };
      promptNode.put(param.getParameterName(), exampleValue);
    }

    return promptNode;
  }
}
