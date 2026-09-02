package org.dcsa.conformance.standards.eblissuance.party;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.party.ScenarioParameters;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public record CarrierScenarioParameters(String carriersX509SigningCertificateInPEMFormat)
  implements ScenarioParameters {

  public static CarrierScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, CarrierScenarioParameters.class);
  }
}
