package org.dcsa.conformance.gateway.scenarios;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

@Getter
public class ConformanceAction {
  protected final UUID id = UUID.randomUUID();
  private final String sourcePartyName;
  private final String targetPartyName;
  protected final ConformanceAction previousAction;
  private final String actionPath;

  public ConformanceAction(
      String sourcePartyName, String targetPartyName, ConformanceAction previousAction, String actionTitle) {
    this.sourcePartyName = sourcePartyName;
    this.targetPartyName = targetPartyName;
    this.previousAction = previousAction;
    this.actionPath = (previousAction == null ? "" : previousAction.actionPath + " - ") + actionTitle;
  }

  public boolean updateFromExchangeIfItMatches(ConformanceExchange exchange) {
    return false;
  }

  protected JsonNode getRequestBody(ConformanceExchange exchange) {
    try {
      return new ObjectMapper().readTree(exchange.getRequestBody());
    } catch (JsonProcessingException e) {
      return new ObjectMapper().createObjectNode();
    }
  }

  public ObjectNode asJsonNode() {
    return new ObjectMapper()
        .createObjectNode()
        .put("actionId", id.toString())
        .put("actionType", getClass().getCanonicalName())
        .put("actionPath", actionPath);
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
