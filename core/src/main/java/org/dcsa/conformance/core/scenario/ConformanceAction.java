package org.dcsa.conformance.core.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.state.StatefulEntity;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

@Getter
public abstract class ConformanceAction implements StatefulEntity {
  private final String sourcePartyName;
  private final String targetPartyName;
  protected final ConformanceAction previousAction;
  private final String actionPath;
  private final String actionTitle;

  protected UUID id = UUID.randomUUID();
  private UUID matchedExchangeUuid;

  public ConformanceAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle) {
    this.sourcePartyName = sourcePartyName;
    this.targetPartyName = targetPartyName;
    this.previousAction = previousAction;
    this.actionTitle = actionTitle;
    this.actionPath =
        (previousAction == null ? "" : previousAction.actionPath + " - ") + actionTitle;
  }

  public void reset() {
    id = UUID.randomUUID();
    matchedExchangeUuid = null;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = new ObjectMapper().createObjectNode();
    jsonState.put("id", id.toString());
    if (matchedExchangeUuid != null) {
      jsonState.put("matchedExchangeUuid", matchedExchangeUuid.toString());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    id = UUID.fromString(jsonState.get("id").asText());
    if (jsonState.has("matchedExchangeUuid")) {
      this.matchedExchangeUuid = UUID.fromString(jsonState.get("matchedExchangeUuid").asText());
    }
  }

  public abstract String getHumanReadablePrompt();

  public JsonNode getJsonForHumanReadablePrompt() {
    return null;
  }

  public boolean isConfirmationRequired() {
    return false;
  }

  public boolean isInputRequired() {
    return false;
  }

  public final void handleExchange(ConformanceExchange exchange) {
    matchedExchangeUuid = exchange.getUuid();
    doHandleExchange(exchange);
  }

  public void doHandleExchange(ConformanceExchange exchange) {}

  public ConformanceCheck createCheck(String expectedApiVersion) {
    return null;
  }

  public void handlePartyInput(JsonNode partyInput) {}

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
