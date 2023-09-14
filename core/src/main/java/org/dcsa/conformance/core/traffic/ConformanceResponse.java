package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record ConformanceResponse(int statusCode, ConformanceMessage message) {

  public ObjectNode toJson() {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode objectNode = objectMapper.createObjectNode();
    objectNode.put("statusCode", statusCode);
    objectNode.set("message", message.toJson());
    return objectNode;
  }

  public static ConformanceResponse fromJson(ObjectNode objectNode) {
    return new ConformanceResponse(
        objectNode.get("statusCode").asInt(),
        ConformanceMessage.fromJson((ObjectNode) objectNode.get("message")));
  }
}
