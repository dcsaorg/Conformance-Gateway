package org.dcsa.conformance.standards.ebl.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.With;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DynamicScenarioParameters(
    @NonNull ScenarioType scenarioType,
    String shippingInstructionsReference,
    String transportDocumentReference,
    JsonNode shippingInstructions,
    JsonNode updatedShippingInstructions,
    boolean newTransportDocumentContent,
    JsonNode transportDocument,
    JsonNode previousTransportDocument) {

  public ObjectNode toJson() {
    return OBJECT_MAPPER.valueToTree(this);
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return new DynamicScenarioParameters(
        ScenarioType.valueOf(jsonNode.required("scenarioType").asText()),
        jsonNode.path("shippingInstructionsReference").asText(null),
        jsonNode.path("transportDocumentReference").asText(null),
        jsonNode.path("shippingInstructions"),
        jsonNode.path("updatedShippingInstructions"),
        jsonNode.path("newTransportDocumentContent").asBoolean(false),
        jsonNode.path("transportDocument"),
        jsonNode.path("previousTransportDocument"));
  }
}
