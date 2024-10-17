package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;

@With
public record SenderScenarioParameters(
    String transportDocumentReference,
    String eblPlatform,
    String senderPublicKeyPEM,
    String carrierPublicKeyPEM)
    implements ScenarioParameters {

  public static SenderScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, SenderScenarioParameters.class);
  }
}
