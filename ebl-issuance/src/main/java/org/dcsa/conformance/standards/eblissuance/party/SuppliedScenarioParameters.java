package org.dcsa.conformance.standards.eblissuance.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.party.ScenarioParameters;

public record SuppliedScenarioParameters(
    String issueToSendToPlatform,
    String issueToPartyName,
    String issueToCodeListProvider,
    String issueToPartyCode,
    String issueToCodeListName,
    String shipperLegalName,
    String shipperCodeListProvider,
    String shipperPartyCode,
    String shipperCodeListName,
    String consigneeOrEndorseeLegalName,
    String consigneeOrEndorseeCodeListProvider,
    String consigneeOrEndorseePartyCode,
    String consigneeOrEndorseeCodeListName,
    String issuingPartyLegalName,
    String issuingPartyCodeListProvider,
    String issuingPartyPartyCode,
    String issuingPartyCodeListName)
    implements ScenarioParameters {

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, SuppliedScenarioParameters.class);
  }
}
