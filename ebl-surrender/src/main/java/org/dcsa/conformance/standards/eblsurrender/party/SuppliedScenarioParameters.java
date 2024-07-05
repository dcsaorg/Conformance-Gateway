package org.dcsa.conformance.standards.eblsurrender.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record SuppliedScenarioParameters(
        String transportDocumentReference, String eblPlatform, String carrierPartyCode, String surrenderPartyCode, String codeListName) {
  public ObjectNode toJson() {
    return new ObjectMapper()
        .createObjectNode()
        .put("transportDocumentReference", transportDocumentReference)
        .put("eblPlatform", eblPlatform)
        .put("carrierPartyCode", surrenderPartyCode)
        .put("surrenderPartyCode", surrenderPartyCode)
        .put("codeListName", codeListName);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
        jsonNode.required("transportDocumentReference").asText(),
        jsonNode.required("eblPlatform").asText(),
        jsonNode.required("carrierPartyCode").asText(),
        jsonNode.required("surrenderPartyCode").asText(),
        jsonNode.required("codeListName").asText());
  }
}
