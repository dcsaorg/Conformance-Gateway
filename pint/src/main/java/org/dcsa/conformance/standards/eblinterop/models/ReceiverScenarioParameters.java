package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

@With
public record ReceiverScenarioParameters(
  String eblPlatform,
  String receiverPartyName,
  String receiverEPUI,
  String receiverEPUICodeListName,
  String receiverPublicKeyPEM
) {
  public ObjectNode toJson() {
    return OBJECT_MAPPER
        .createObjectNode()
        .put("eblPlatform", eblPlatform)
        .put("receiverPartyName", receiverPartyName)
        .put("receiverEPUI", receiverEPUI)
        .put("receiverEPUICodeListName", receiverEPUICodeListName)
        .put("receiverPublicKeyPEM", receiverPublicKeyPEM);
  }

  public static ReceiverScenarioParameters fromJson(JsonNode jsonNode) {
    return new ReceiverScenarioParameters(
      jsonNode.required("eblPlatform").asText(),
      jsonNode.required("receiverPartyName").asText(),
      jsonNode.required("receiverEPUI").asText(),
      jsonNode.required("receiverEPUICodeListName").asText(),
      jsonNode.required("receiverPublicKeyPEM").asText()
    );
  }
}
