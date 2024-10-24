package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public record ConformanceResponse(int statusCode, ConformanceMessage message) {

  public ObjectNode toJson() {
    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
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
