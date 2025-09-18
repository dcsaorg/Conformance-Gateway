package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EblDynamicScenarioParameters(
    String scenarioType,
    String shippingInstructionsReference,
    String transportDocumentReference,
    JsonNode shippingInstructions,
    JsonNode updatedShippingInstructions,
    boolean newTransportDocumentContent)
    implements ScenarioParameters {

  public static EblDynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return new EblDynamicScenarioParameters(
        jsonNode.path("scenarioType").asText(null),
        jsonNode.path("shippingInstructionsReference").asText(null),
        jsonNode.path("transportDocumentReference").asText(null),
        jsonNode.path("shippingInstructions"),
        jsonNode.path("updatedShippingInstructions"),
        jsonNode.path("newTransportDocumentContent").asBoolean(false));
  }
}
