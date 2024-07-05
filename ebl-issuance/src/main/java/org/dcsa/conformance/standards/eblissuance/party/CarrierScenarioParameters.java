package org.dcsa.conformance.standards.eblissuance.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CarrierScenarioParameters(
  String carrierSigningKeyPEM
  ) {
  public ObjectNode toJson() {
    return new ObjectMapper()
        .createObjectNode()
        .put("carrierSigningKeyPEM", carrierSigningKeyPEM);
  }

  public static CarrierScenarioParameters fromJson(JsonNode jsonNode) {
    return new CarrierScenarioParameters(
        jsonNode.required("carrierSigningKeyPEM").asText());
  }
}
