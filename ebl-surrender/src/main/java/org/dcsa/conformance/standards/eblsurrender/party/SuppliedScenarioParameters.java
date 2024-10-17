package org.dcsa.conformance.standards.eblsurrender.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.party.ScenarioParameters;

public record SuppliedScenarioParameters(
    String transportDocumentReference,
    JsonNode issueToParty,
    JsonNode carrierParty,
    JsonNode surrendereeParty)
    implements ScenarioParameters {

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, SuppliedScenarioParameters.class);
  }
}
