package org.dcsa.conformance.standards.eblinterop.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@With
public record SenderScenarioParameters(String transportDocumentReference) {
  public ObjectNode toJson() {
    return OBJECT_MAPPER
        .createObjectNode()
        .put("transportDocumentReference", transportDocumentReference);
  }

  public static SenderScenarioParameters fromJson(JsonNode jsonNode) {
    return new SenderScenarioParameters(
      jsonNode.required("transportDocumentReference").asText()
    );
  }
}
