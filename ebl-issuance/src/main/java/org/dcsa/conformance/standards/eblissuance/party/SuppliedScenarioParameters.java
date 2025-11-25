package org.dcsa.conformance.standards.eblissuance.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.party.ScenarioParameters;

public record SuppliedScenarioParameters(
    String issueToSendToPlatform,
    String issueToPartyName,
    String issueToPartyCode,
    String issueToCodeListName,
    String consigneeOrEndorseeLegalName,
    String consigneeOrEndorseePartyCode,
    String consigneeOrEndorseeCodeListName)
    implements ScenarioParameters {

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, SuppliedScenarioParameters.class);
  }
}
