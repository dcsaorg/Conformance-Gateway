package org.dcsa.conformance.standards.eblsurrender.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record SuppliedScenarioParameters(
      String transportDocumentReference,
      JsonNode issueToParty,
      JsonNode carrierParty,
      JsonNode surrendereeParty
) {
  public ObjectNode toJson() {
    return OBJECT_MAPPER.valueToTree(this);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, SuppliedScenarioParameters.class);
  }
}
