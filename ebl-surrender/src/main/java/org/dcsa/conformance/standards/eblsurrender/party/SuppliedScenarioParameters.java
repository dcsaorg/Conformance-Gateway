package org.dcsa.conformance.standards.eblsurrender.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record SuppliedScenarioParameters(
    String transportDocumentReference, String eblPlatform, String partyCode, String codeListName) {
  public ObjectNode toJson() {
    return new ObjectMapper()
        .createObjectNode()
        .put("transportDocumentReference", transportDocumentReference)
        .put("eblPlatform", eblPlatform)
        .put("partyCode", partyCode)
        .put("codeListName", codeListName);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
        jsonNode.required("transportDocumentReference").asText(),
        jsonNode.required("eblPlatform").asText(),
        jsonNode.required("partyCode").asText(),
        jsonNode.required("codeListName").asText());
  }
}
