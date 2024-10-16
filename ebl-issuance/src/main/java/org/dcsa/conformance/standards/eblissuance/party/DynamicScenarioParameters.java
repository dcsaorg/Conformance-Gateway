package org.dcsa.conformance.standards.eblissuance.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;
import org.dcsa.conformance.standards.eblissuance.action.EblType;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@With
public record DynamicScenarioParameters(
  EblType eblType) {
  public ObjectNode toJson() {
    return OBJECT_MAPPER
        .createObjectNode()
        .put("eblType", eblType.name());
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return new DynamicScenarioParameters(
        EblType.valueOf(jsonNode.required("eblType").asText())
    );
  }
}
