package org.dcsa.conformance.gateway.scenarios;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

@AllArgsConstructor
@Getter
public class ConformanceAction {
  protected final UUID id = UUID.randomUUID();
  private final String sourcePartyName;
  private final String targetPartyName;

  public boolean trafficExchangeMatches(ConformanceExchange exchange) {
    return false;
  }

  protected JsonNode getRequestBody(ConformanceExchange exchange) {
    try {
      return new ObjectMapper().readTree(exchange.getRequestBody());
    } catch (JsonProcessingException e) {
      return new ObjectMapper().createObjectNode();
    }
  }

  protected boolean stringAttributeEquals(JsonNode jsonNode, String name, String value) {
    return jsonNode.has(name) && Objects.equals(value, jsonNode.get(name).asText());
  }

  public ObjectNode asJsonNode() {
    return new ObjectMapper()
        .createObjectNode()
        .put("actionId", id.toString())
        .put("actionType", getClass().getCanonicalName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConformanceAction that = (ConformanceAction) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
