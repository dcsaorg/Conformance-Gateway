package org.dcsa.conformance.standards.cs.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.With;

@With
public record DynamicScenarioParameters(
  String cursor
)  {

  public ObjectNode toJson() {
    ObjectNode dspNode = new ObjectMapper().createObjectNode();
    if (cursor != null) {
      dspNode.put("cursor", cursor);
    }
    return dspNode;
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode dspNode = (ObjectNode) jsonNode;
    return new DynamicScenarioParameters(dspNode.path("cursor").asText()
    );
  }
}
