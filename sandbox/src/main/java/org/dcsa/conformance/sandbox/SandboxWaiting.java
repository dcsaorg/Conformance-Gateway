package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public record SandboxWaiting(String who, String forWhom, String toDoWhat) {
  ObjectNode toJson() {
    return OBJECT_MAPPER
        .createObjectNode()
        .put("who", who)
        .put("forWhom", forWhom)
        .put("toDoWhat", toDoWhat);
  }

  static SandboxWaiting fromJson(ObjectNode objectNode) {
    return new SandboxWaiting(
        objectNode.get("who").asText(),
        objectNode.get("forWhom").asText(),
        objectNode.get("toDoWhat").asText());
  }
}
