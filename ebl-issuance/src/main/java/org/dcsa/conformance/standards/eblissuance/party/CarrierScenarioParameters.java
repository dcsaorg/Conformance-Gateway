package org.dcsa.conformance.standards.eblissuance.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CarrierScenarioParameters(
  String carrierSigningKeyPEM
  ) {
  public ObjectNode toJson() {
    return OBJECT_MAPPER
        .createObjectNode()
        .put("carrierSigningKeyPEM", carrierSigningKeyPEM);
  }

  public static CarrierScenarioParameters fromJson(JsonNode jsonNode) {
    return new CarrierScenarioParameters(
        jsonNode.required("carrierSigningKeyPEM").asText());
  }
}
