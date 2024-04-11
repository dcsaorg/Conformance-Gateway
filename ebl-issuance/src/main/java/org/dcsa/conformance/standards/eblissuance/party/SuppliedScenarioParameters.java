package org.dcsa.conformance.standards.eblissuance.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record SuppliedScenarioParameters(
  String sendToPlatform,
  String issueToLegalName,
  String issueToPartyCode,
  String issueToCodeListName,
  String consigneeOrEndorseeLegalName,
  String consigneeOrEndorseePartyCode,
  String consigneeOrEndorseeCodeListName
  ) {
  public ObjectNode toJson() {
    return new ObjectMapper()
        .createObjectNode()
        .put("sendToPlatform", sendToPlatform)
        .put("issueToLegalName", issueToLegalName)
        .put("issueToPartyCode", issueToPartyCode)
        .put("issueToCodeListName", issueToCodeListName)
        .put("consigneeOrEndorseeLegalName", consigneeOrEndorseeLegalName)
        .put("consigneeOrEndorseePartyCode", consigneeOrEndorseePartyCode)
        .put("consigneeOrEndorseeCodeListName", consigneeOrEndorseeCodeListName);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
        jsonNode.required("sendToPlatform").asText(),
        jsonNode.required("issueToLegalName").asText(),
        jsonNode.required("issueToPartyCode").asText(),
        jsonNode.required("issueToCodeListName").asText(),
        jsonNode.required("consigneeOrEndorseeLegalName").asText(),
        jsonNode.required("consigneeOrEndorseePartyCode").asText(),
        jsonNode.required("consigneeOrEndorseeCodeListName").asText());
  }
}
