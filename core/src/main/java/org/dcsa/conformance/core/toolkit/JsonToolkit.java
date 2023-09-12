package org.dcsa.conformance.core.toolkit;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public enum JsonToolkit {
  ; // no instances

  public static boolean stringAttributeEquals(JsonNode jsonNode, String name, String value) {
    return jsonNode.has(name) && Objects.equals(value, jsonNode.get(name).asText());
  }

  public static String getTextAttributeOrNull(JsonNode jsonNode, String attributeName) {
    JsonNode attributeNode = jsonNode.get(attributeName);
    return attributeNode == null ? null : attributeNode.asText();
  }
}
