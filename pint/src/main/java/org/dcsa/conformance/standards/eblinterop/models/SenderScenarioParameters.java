package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

@With
public record SenderScenarioParameters(
    String transportDocumentReference,
    String eblPlatform,
    String senderPublicKeyPEM,
    String carrierPublicKeyPEM) {

  public ObjectNode toJson() {
    return OBJECT_MAPPER.valueToTree(this);
  }

  public static SenderScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, SenderScenarioParameters.class);
  }
}
