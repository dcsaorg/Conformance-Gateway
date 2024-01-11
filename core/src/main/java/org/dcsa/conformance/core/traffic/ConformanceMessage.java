package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

import java.util.Collection;
import java.util.Map;

import static org.dcsa.conformance.core.Util.STATE_OBJECT_MAPPER;

public record ConformanceMessage(
    String sourcePartyName,
    String sourcePartyRole,
    String targetPartyName,
    String targetPartyRole,
    Map<String, ? extends Collection<String>> headers,
    ConformanceMessageBody body,
    long timestamp) {

  public ObjectNode toJson() {
    ObjectNode objectNode = STATE_OBJECT_MAPPER.createObjectNode();
    objectNode.put("sourcePartyName", sourcePartyName);
    objectNode.put("sourcePartyRole", sourcePartyRole);
    objectNode.put("targetPartyName", targetPartyName);
    objectNode.put("targetPartyRole", targetPartyRole);
    objectNode.set("headers", JsonToolkit.mapOfStringToStringCollectionToJson(headers));
    objectNode.set("body", body.toJson());
    objectNode.put("timestamp", timestamp);
    return objectNode;
  }

  public static ConformanceMessage fromJson(ObjectNode objectNode) {
    return new ConformanceMessage(
        objectNode.get("sourcePartyName").asText(),
        objectNode.get("sourcePartyRole").asText(),
        objectNode.get("targetPartyName").asText(),
        objectNode.get("targetPartyRole").asText(),
        JsonToolkit.mapOfStringToStringCollectionFromJson((ArrayNode) objectNode.get("headers")),
        ConformanceMessageBody.fromJson((ObjectNode) objectNode.get("body")),
        objectNode.get("timestamp").asLong());
  }
}
