package org.dcsa.conformance.standards.tnt.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.With;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@With
public record DynamicScenarioParameters(
  String cursor,
  String firstPage,
  String lastPage,
  String nextPage,
  String prevPage) {

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode dspNode = (ObjectNode) jsonNode;
    return new DynamicScenarioParameters(
        dspNode.path("cursor").asText(),
        dspNode.path("firstPage").asText(),
        dspNode.path("lastPage").asText(),
      dspNode.path("prevPage").asText(),
      dspNode.path("nextPage").asText());
  }

  public ObjectNode toJson() {
    ObjectNode dspNode = OBJECT_MAPPER.createObjectNode();
    if (cursor != null) {
      dspNode.put("cursor", cursor);
    }
    if (firstPage != null) {
      dspNode.put("firstPage", firstPage);
    }
    if (lastPage != null) {
      dspNode.put("lastPage", lastPage);
    }
    if (nextPage != null) {
      dspNode.put("nextPage", nextPage);
    }
    if (prevPage != null) {
      dspNode.put("prevPage", prevPage);
    }
    return dspNode;
  }
}
