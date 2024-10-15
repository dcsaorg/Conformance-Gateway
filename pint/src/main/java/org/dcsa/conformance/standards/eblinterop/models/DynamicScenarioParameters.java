package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import lombok.With;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DynamicScenarioParameters(
    String transportDocumentChecksum,
    int documentCount,
    Set<String> documentChecksums,
    String envelopeReference,
    JsonNode receiverValidation) {

  public ObjectNode toJson() {
    JsonNode jsonNode = OBJECT_MAPPER.valueToTree(this);
    return (ObjectNode) jsonNode;
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, DynamicScenarioParameters.class);
  }
}
