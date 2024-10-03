package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@With
public record DynamicScenarioParameters(
  String transportDocumentChecksum,
  int documentCount,
  Set<String> documentChecksums,
  String envelopeReference,
  JsonNode receiverValidation
) {
  public ObjectNode toJson() {
    var node = OBJECT_MAPPER.createObjectNode()
      .put("transportDocumentChecksum", transportDocumentChecksum)
      .put("documentCount", documentCount)
      .put("envelopeReference", envelopeReference);
    var jsonDocumentChecksums = node.putArray("documentChecksums");
    for (var checksum : documentChecksums) {
      jsonDocumentChecksums.add(checksum);
    }
    node.set("receiverValidation", receiverValidation);
    return node;
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return new DynamicScenarioParameters(
      jsonNode.required("transportDocumentChecksum").asText(null),
      jsonNode.required("documentCount").asInt(),
      StreamSupport.stream(jsonNode.required("documentChecksums").spliterator(), false)
        .map(JsonNode::asText)
        .collect(Collectors.toUnmodifiableSet()),
      jsonNode.required("envelopeReference").asText(),
      jsonNode.path("receiverValidation")
    );
  }
}
