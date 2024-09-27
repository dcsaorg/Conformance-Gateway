package org.dcsa.conformance.standards.cs.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

@With
public record DynamicScenarioParameters(String cursor, String firstPage, String secondPage) {

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode dspNode = (ObjectNode) jsonNode;
    return new DynamicScenarioParameters(
        dspNode.path("cursor").asText(),
        dspNode.path("firstPage").asText(),
        dspNode.path("secondPage").asText());
  }

  public ObjectNode toJson() {
    ObjectNode dspNode = OBJECT_MAPPER.createObjectNode();
    if (cursor != null) {
      dspNode.put("cursor", cursor);
    }
    if (firstPage != null) {
      dspNode.put("firstPage", firstPage);
    }
    if (secondPage != null) {
      dspNode.put("secondPage", secondPage);
    }
    return dspNode;
  }
}
