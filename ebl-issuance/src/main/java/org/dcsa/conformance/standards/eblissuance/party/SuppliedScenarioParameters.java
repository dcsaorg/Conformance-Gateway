package org.dcsa.conformance.standards.eblissuance.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.party.ScenarioParameters;

@JsonInclude(JsonInclude.Include.NON_NULL)
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

  public static SuppliedScenarioParameters sandboxDefaults() {
    return new SuppliedScenarioParameters(
        "DCSA",
        "DCSA issue to party",
        "W3C",
        "1234-issue-to",
        "DCSA",
        "DCSA Shipper",
        "W3C",
        "5677-cn-or-end",
        "DCSA",
        "DCSA Consignee/Endorsee",
        "W3C",
        "5678-cn-or-end",
        "DCSA",
        "DCSA Issuing Party",
        "W3C",
        "5679-cn-or-end",
        "DCSA");
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, SuppliedScenarioParameters.class);
  }
}
