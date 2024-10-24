package org.dcsa.conformance.core.traffic;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.Map;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

public record ConformanceMessage(
    String sourcePartyName,
    String sourcePartyRole,
    String targetPartyName,
    String targetPartyRole,
    Map<String, ? extends Collection<String>> headers,
    ConformanceMessageBody body,
    long timestamp) {

  public ObjectNode toJson() {
    ObjectNode objectNode = OBJECT_MAPPER.valueToTree(this);
    // Apply custom serialization
    objectNode.set("headers", JsonToolkit.mapOfStringToStringCollectionToJson(headers));
    objectNode.set("body", body.toJson());
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
