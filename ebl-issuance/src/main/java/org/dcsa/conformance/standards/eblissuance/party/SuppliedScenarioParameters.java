package org.dcsa.conformance.standards.eblissuance.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record SuppliedScenarioParameters(
    String sendToPlatform, String partyCode, String codeListName) {
  public ObjectNode toJson() {
    return new ObjectMapper()
        .createObjectNode()
        .put("sendToPlatform", sendToPlatform)
        .put("partyCode", partyCode)
        .put("codeListName", codeListName);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
        jsonNode.required("sendToPlatform").asText(),
        jsonNode.required("partyCode").asText(),
        jsonNode.required("codeListName").asText());
  }
}
