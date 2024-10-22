package org.dcsa.conformance.sandbox;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record SandboxWaiting(String who, String forWhom, String toDoWhat) {

  public ObjectNode toJson() {
    return OBJECT_MAPPER.valueToTree(this);
  }

  public static SandboxWaiting fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, SandboxWaiting.class);
  }
}
