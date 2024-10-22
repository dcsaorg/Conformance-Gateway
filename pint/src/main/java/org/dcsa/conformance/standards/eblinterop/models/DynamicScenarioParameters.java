package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DynamicScenarioParameters(
    String transportDocumentChecksum,
    int documentCount,
    Set<String> documentChecksums,
    String envelopeReference,
    JsonNode receiverValidation)
    implements ScenarioParameters {

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, DynamicScenarioParameters.class);
  }
}
