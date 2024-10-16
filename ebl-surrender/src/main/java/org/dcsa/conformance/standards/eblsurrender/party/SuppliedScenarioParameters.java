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
    var r = OBJECT_MAPPER
        .createObjectNode()
        .put("transportDocumentReference", transportDocumentReference);

    r.set("issueToParty", issueToParty);
    r.set("carrierParty", carrierParty);
    r.set("surrendereeParty", surrendereeParty);
    return r;
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
        jsonNode.required("transportDocumentReference").asText(),
        jsonNode.required("issueToParty"),
        jsonNode.required("carrierParty"),
        jsonNode.required("surrendereeParty"));
  }
}
