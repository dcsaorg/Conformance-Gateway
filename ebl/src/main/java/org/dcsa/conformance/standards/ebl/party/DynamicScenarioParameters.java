package org.dcsa.conformance.standards.ebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

@With
public record DynamicScenarioParameters(
    String shippingInstructionsReference,
    String transportDocumentReference,
    ShippingInstructionsStatus shippingInstructionsStatus,
    ShippingInstructionsStatus updatedShippingInstructionsStatus,
    TransportDocumentStatus transportDocumentStatus) {
  public ObjectNode toJson() {
    ObjectNode dspNode = new ObjectMapper().createObjectNode();
    if (shippingInstructionsReference != null) {
      dspNode.put("shippingInstructionsReference", shippingInstructionsReference);
    }
    if (transportDocumentReference != null) {
      dspNode.put("transportDocumentReference", transportDocumentReference);
    }
    if (shippingInstructionsStatus != null) {
      dspNode.put("shippingInstructionsStatus", shippingInstructionsStatus.wireName());
    }
    if (updatedShippingInstructionsStatus != null) {
      dspNode.put(
          "updatedShippingInstructionsStatus", updatedShippingInstructionsStatus.wireName());
    }
    if (transportDocumentStatus != null) {
      dspNode.put("transportDocumentStatus", transportDocumentStatus.wireName());
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
            : null,
        dspNode.has("shippingInstructionsStatus")
            ? ShippingInstructionsStatus.fromWireName(
                dspNode.get("shippingInstructionsStatus").asText())
            : null,
        dspNode.has("updatedShippingInstructionsStatus")
            ? ShippingInstructionsStatus.fromWireName(
                dspNode.get("updatedShippingInstructionsStatus").asText())
            : null,
        dspNode.has("transportDocumentStatus")
            ? TransportDocumentStatus.fromWireName(dspNode.get("transportDocumentStatus").asText())
            : null);
  }
}
