package org.dcsa.conformance.standards.ebl.party;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;
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
    @Deprecated JsonNode transportDocument,
    @Deprecated JsonNode previousTransportDocument)
    implements ScenarioParameters {

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
