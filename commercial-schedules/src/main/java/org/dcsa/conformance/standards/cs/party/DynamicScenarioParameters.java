package org.dcsa.conformance.standards.cs.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DynamicScenarioParameters {

  String nextPage;

  public ObjectNode toJson() {
    ObjectNode dspNode = new ObjectMapper().createObjectNode();
    if (nextPage != null) {
      dspNode.put("next", nextPage);
    }
    return dspNode;
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode dspNode = (ObjectNode) jsonNode;
    return new DynamicScenarioParameters(dspNode.path("next").asText()
    );
  }
}
