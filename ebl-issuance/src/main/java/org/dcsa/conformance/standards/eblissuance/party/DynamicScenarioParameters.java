package org.dcsa.conformance.standards.eblissuance.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;
import org.dcsa.conformance.standards.eblissuance.action.EblType;

@With
public record DynamicScenarioParameters(
  EblType eblType) {
  public ObjectNode toJson() {
    return new ObjectMapper()
        .createObjectNode()
        .put("eblType", eblType.name());
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return new DynamicScenarioParameters(
        EblType.valueOf(jsonNode.required("eblType").asText())
    );
  }
}
