package org.dcsa.conformance.standards.ebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

@With
public record DynamicScenarioParameters(
  String shippingInstructionsReference, String transportDocumentReference) {
  public ObjectNode toJson() {
    ObjectNode dspNode = new ObjectMapper().createObjectNode();
    if (shippingInstructionsReference != null) {
      dspNode.put("shippingInstructionsReference", shippingInstructionsReference);
    }
    if (transportDocumentReference != null) {
      dspNode.put("transportDocumentReference", transportDocumentReference);
    }
    return dspNode;
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode dspNode = (ObjectNode) jsonNode;
    return new DynamicScenarioParameters(
        dspNode.has("shippingInstructionsReference")
            ? dspNode.get("shippingInstructionsReference").asText()
            : null,
        dspNode.has("transportDocumentReference")
            ? dspNode.get("transportDocumentReference").asText()
            : null);
  }
}
