package org.dcsa.conformance.standards.portcall.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.portcall.party.PortCallFilterParameter;
import org.dcsa.conformance.standards.portcall.party.ScenarioType;
import org.dcsa.conformance.standards.portcall.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends PortCallAction {

  private static final String SUPPLIED_SCENARIO_PARAMETERS = "suppliedScenarioParameters";

  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private final ScenarioType scenarioType;
  private final Set<PortCallFilterParameter> explicitFilterParameters;

  public SupplyScenarioParametersAction(String producerPartyName, ScenarioType scenarioType) {
    super(producerPartyName, null, null, "Supply parameters (%s)".formatted(scenarioType.getLabel()));
    this.scenarioType = scenarioType;
    this.explicitFilterParameters = Set.of();
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
  }

  public SupplyScenarioParametersAction(String producerPartyName, PortCallFilterParameter... filterParameters) {
    super(producerPartyName, null, null,
        "Supply parameters (%s)"
            .formatted(
                Arrays.stream(filterParameters)
                    .map(PortCallFilterParameter::getQueryParamName)
                    .collect(Collectors.joining(" + "))));
    this.scenarioType = null;
    this.explicitFilterParameters = new LinkedHashSet<>(Arrays.asList(filterParameters));
  }

  @Override
  public String getHumanReadablePrompt() {
    return scenarioType != null
        ? "Using the example format below, provide any optional query parameter for which your system returns, via GET /events, at least one Port Call event that demonstrates %s."
            .formatted(scenarioType.getLabel())
        : "Using the example format below, provide the query parameters for which your system supports pagination on GET /events.";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return explicitFilterParameters.isEmpty()
        ? examplePrompt()
        : examplePrompt(explicitFilterParameters);
  }

  @Override
  public void doHandlePartyInput(JsonNode partyInput) {
    JsonNode partyInputNode = partyInput.get("input");
    if (partyInputNode != null && !partyInputNode.isNull()) {
      suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInputNode);
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode objectNode = super.asJsonNode();
    if (scenarioType != null) {
      objectNode.put("scenarioType", scenarioType.name());
    }
    if (!explicitFilterParameters.isEmpty()) {
      ArrayNode filterParametersNode = objectNode.putArray("filterParameters");
      explicitFilterParameters.forEach(param -> filterParametersNode.add(param.getQueryParamName()));
    }
    return objectNode;
  }

  @Override
  public void reset() {
    super.reset();
    suppliedScenarioParameters = null;
    if (scenarioType != null) {
      this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
    }
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
      suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(jsonState.required(SUPPLIED_SCENARIO_PARAMETERS));
    }
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  public static ObjectNode examplePrompt() {
    return JsonToolkit.OBJECT_MAPPER
        .createObjectNode()
        .put("UNLocationCode", "NLRTM")
        .put("portVisitReference", "NLAMS1234589")
        .put("carrierServiceName", "Great Lion Service")
        .put("carrierServiceCode", "FE1")
        .put("universalServiceReference", "SR12345A")
        .put("terminalCallReference", "15063401")
        .put("carrierImportVoyageNumber", "1234N")
        .put("universalImportVoyageReference", "2301W")
        .put("carrierExportVoyageNumber", "1234N")
        .put("universalExportVoyageReference", "2301W")
        .put("portCallServiceTypeCode", "BERTH")
        .put("vesselIMONumber", "12345678")
        .put("vesselName", "King of the Seas")
        .put("vesselMMSINumber", "278111222")
        .put("portCallID", "0342254a-5927-4856-b9c9-aa12e7c00563")
        .put("terminalCallID", "0342254a-5927-4856-b9c9-aa12e7c00563")
        .put("portCallServiceID", "0342254a-5927-4856-b9c9-aa12e7c00563")
        .put("timestampID", "0342254a-5927-4856-b9c9-aa12e7c00563")
        .put("classifierCode", "ACT")
        .put("eventTimestampMin", "2025-01-23T01:23:45Z")
        .put("eventTimestampMax", "2025-01-23T01:23:45Z")
        .put("limit", "10");
  }

  @Override
  public Map<String, Boolean> getExpectedInputAttributes() {
    if (explicitFilterParameters!=null && !explicitFilterParameters.isEmpty()) {
      return super.getExpectedInputAttributes();
    }
    return Map.of();
  }

  public static ObjectNode examplePrompt(Set<PortCallFilterParameter> filterParameters) {
    ObjectNode fullPrompt = examplePrompt();
    ObjectNode promptNode = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    for (PortCallFilterParameter param : filterParameters) {
      String queryParamName = param.getQueryParamName();
      if (fullPrompt.has(queryParamName)) {
        promptNode.set(queryParamName, fullPrompt.get(queryParamName));
      } else if (param == PortCallFilterParameter.CURSOR) {
        promptNode.put(queryParamName, "ExampleNextPageCursor");
      }
    }
    return promptNode;
  }
}

