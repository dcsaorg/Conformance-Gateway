package org.dcsa.conformance.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import lombok.experimental.UtilityClass;

import java.util.Objects;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

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

  public static JsonNode trimRootArrayByLimit(JsonNode responseBody, String limitValue) {
    if (!responseBody.isArray() || limitValue == null || limitValue.isBlank()) {
      return responseBody;
    }

    final int limit;
    try {
      limit = Integer.parseInt(limitValue);
    } catch (NumberFormatException e) {
      return responseBody;
    }

    if (limit < 0) {
      return responseBody;
    }

    ArrayNode source = (ArrayNode) responseBody;
    if (source.size() <= limit) {
      return responseBody;
    }

    ArrayNode trimmed = OBJECT_MAPPER.createArrayNode();
    for (int i = 0; i < limit; i++) {
      trimmed.add(source.get(i));
    }
    return trimmed;
  }
}
