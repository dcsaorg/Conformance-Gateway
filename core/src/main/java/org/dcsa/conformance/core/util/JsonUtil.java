package org.dcsa.conformance.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import lombok.experimental.UtilityClass;

import java.util.Objects;

@UtilityClass
public class JsonUtil {

  public static boolean isMissingOrEmpty(JsonNode node) {
    if (node instanceof ValueNode) {
      return node.isMissingNode() || node.isNull() || node.asText().isBlank();
    }
    return Objects.isNull(node) || node.isMissingNode() || node.isNull() || node.isEmpty();
  }

  public static boolean isMissing(JsonNode node) {
    return Objects.isNull(node) || node.isMissingNode() || node.isNull();
  }
}
