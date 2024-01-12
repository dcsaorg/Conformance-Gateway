package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.Map;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public record ConformanceWebRequest(
    String method,
    String url,
    Map<String, ? extends Collection<String>> queryParameters,
    Map<String, ? extends Collection<String>> headers,
    String body) {

  public ObjectNode toJson() {
    ObjectNode objectNode =
      OBJECT_MAPPER
            .createObjectNode()
            .put("method", method)
            .put("url", url)
            .put("body", body);
    objectNode.set(
        "queryParameters", JsonToolkit.mapOfStringToStringCollectionToJson(queryParameters));
    objectNode.set("headers", JsonToolkit.mapOfStringToStringCollectionToJson(headers));
    return objectNode;
  }

  public static ConformanceWebRequest fromJson(ObjectNode objectNode) {
    return new ConformanceWebRequest(
        objectNode.get("method").asText(),
        objectNode.get("url").asText(),
        JsonToolkit.mapOfStringToStringCollectionFromJson(
            (ArrayNode) objectNode.get("queryParameters")),
        JsonToolkit.mapOfStringToStringCollectionFromJson((ArrayNode) objectNode.get("headers")),
        objectNode.get("url").asText());
  }
}
