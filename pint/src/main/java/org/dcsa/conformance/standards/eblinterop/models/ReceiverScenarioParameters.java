package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReceiverScenarioParameters(JsonNode receiverParty, String receiverPublicKeyPEM) {

  public ObjectNode toJson() {
    return OBJECT_MAPPER.valueToTree(this);
  }

  public static ReceiverScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, ReceiverScenarioParameters.class);
  }
}
