package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.Map;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

public record ConformanceRequest(
    String method,
    String url,
    Map<String, ? extends Collection<String>> queryParams,
    ConformanceMessage message) {

  public ConformanceResponse createResponse(
      int statusCode,
      Map<String, ? extends Collection<String>> headers,
      ConformanceMessageBody body) {
    return new ConformanceResponse(
        statusCode,
        new ConformanceMessage(
            this.message.targetPartyName(),
            this.message.targetPartyRole(),
            this.message.sourcePartyName(),
            this.message.sourcePartyRole(),
            headers,
            body,
            System.currentTimeMillis()));
  }

  public ObjectNode toJson() {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode objectNode = objectMapper.createObjectNode();
    objectNode.put("method", method);
    objectNode.put("url", url);
    objectNode.set("queryParams", JsonToolkit.mapOfStringToStringCollectionToJson(queryParams));
    objectNode.set("message", message.toJson());
    return objectNode;
  }

  public static ConformanceRequest fromJson(ObjectNode objectNode) {
    return new ConformanceRequest(
        objectNode.get("method").asText(),
        objectNode.get("url").asText(),
        JsonToolkit.mapOfStringToStringCollectionFromJson((ArrayNode) objectNode.get("queryParams")),
        ConformanceMessage.fromJson((ObjectNode) objectNode.get("message")));
  }

}
