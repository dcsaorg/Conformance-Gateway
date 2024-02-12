package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

@With
public record ReceiverScenarioParameters(
  String eblPlatform,
  String receiverLegalName,
  String receiverEPUI,
  String receiverEPUICodeListName
) {
  public ObjectNode toJson() {
    return OBJECT_MAPPER
        .createObjectNode()
        .put("eblPlatform", eblPlatform)
        .put("receiverLegalName", receiverLegalName)
        .put("receiverEPUI", receiverEPUI)
        .put("receiverEPUICodeListName", receiverEPUICodeListName);
  }

  public static ReceiverScenarioParameters fromJson(JsonNode jsonNode) {
    return new ReceiverScenarioParameters(
      jsonNode.required("eblPlatform").asText(),
      jsonNode.required("receiverLegalName").asText(),
      jsonNode.required("receiverEPUI").asText(),
      jsonNode.required("receiverEPUICodeListName").asText()
    );
  }
}
