package org.dcsa.conformance.standards.cs.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.With;

@With
public record DynamicScenarioParameters(
  String cursor,
  String firstPage,
  String secondPage
)  {

  public ObjectNode toJson() {
    ObjectNode dspNode = new ObjectMapper().createObjectNode();
    if (cursor != null) {
      dspNode.put("cursor", cursor);
    }
    if(firstPage != null){
      dspNode.put("jsonResponse", firstPage);
    }
    if(secondPage != null){
      dspNode.put("secondPage", secondPage);
    }
    return dspNode;
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode dspNode = (ObjectNode) jsonNode;
    return new DynamicScenarioParameters(dspNode.path("cursor").asText(),dspNode.path("firstPage").asText(),dspNode.path("secondPage").asText());
  }
}
