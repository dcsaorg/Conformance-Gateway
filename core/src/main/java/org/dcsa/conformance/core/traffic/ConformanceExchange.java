package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class ConformanceExchange {
  private final UUID uuid;
  private final ConformanceRequest request;
  private final ConformanceResponse response;

  private ConformanceExchange(UUID uuid, ConformanceRequest request, ConformanceResponse response) {
    this.uuid = uuid;
    this.request = request;
    this.response = response;
  }

  public ConformanceExchange(ConformanceRequest request, ConformanceResponse response) {
    this(UUID.randomUUID(), request, response);
  }

  public ObjectNode toJson() {
    ObjectNode jsonState = new ObjectMapper().createObjectNode();
    jsonState.put("uuid", uuid.toString());
    jsonState.set("request", request.toJson());
    jsonState.set("response", response.toJson());
    return jsonState;
  }

  public static ConformanceExchange fromJson(ObjectNode objectNode) {
    return new ConformanceExchange(
        UUID.fromString(objectNode.get("uuid").asText()),
        ConformanceRequest.fromJson((ObjectNode) objectNode.get("request")),
        ConformanceResponse.fromJson((ObjectNode) objectNode.get("response")));
  }

  public ConformanceMessage getMessage(HttpMessageType httpMessageType) {
    return switch (httpMessageType) {
      case REQUEST -> request.message();
      case RESPONSE -> response.message();
    };
  }
}
