package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

@With
public record ReceiverScenarioParameters(
  JsonNode receiverParty,
  String receiverPublicKeyPEM
) {
  public ObjectNode toJson() {
    var r = OBJECT_MAPPER
        .createObjectNode()
        .put("receiverPublicKeyPEM", receiverPublicKeyPEM);
    r.set("receiverParty", receiverParty);
    return r;
  }

  public static ReceiverScenarioParameters fromJson(JsonNode jsonNode) {
    return new ReceiverScenarioParameters(
      jsonNode.required("receiverParty"),
      jsonNode.required("receiverPublicKeyPEM").asText()
    );
  }
}
