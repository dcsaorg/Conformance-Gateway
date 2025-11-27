package org.dcsa.conformance.standards.portcall.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.portcall.party.ScenarioType;
import org.dcsa.conformance.standards.portcall.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends PortCallAction {

  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private final ScenarioType scenarioType;

  public SupplyScenarioParametersAction(String publisherPartyName, ScenarioType scenarioType) {
    super(
        publisherPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s)".formatted(scenarioType.name()));
    this.scenarioType = scenarioType;
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Using the example format below, provide one or more query parameters"
        + " for which the internal sandbox can GET Port Call Events from your system";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return examplePrompt();
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    JsonNode partyInputNode = partyInput.get("input");
    if (partyInputNode != null && !partyInputNode.isNull()) {
      suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInputNode);
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("scenarioType", scenarioType.name());
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
}
