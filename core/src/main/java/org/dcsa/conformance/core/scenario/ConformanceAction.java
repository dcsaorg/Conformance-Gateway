package org.dcsa.conformance.core.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import org.dcsa.conformance.core.state.StatefulEntity;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

@Getter
public abstract class ConformanceAction implements StatefulEntity {
  protected UUID id = UUID.randomUUID();
  private final String sourcePartyName;
  private final String targetPartyName;
  protected final ConformanceAction previousAction;
  private final String actionPath;
  private final String actionTitle;

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

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = new ObjectMapper().createObjectNode();
    jsonState.put("id", id.toString());
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    id = UUID.fromString(jsonState.get("id").asText());
  }

  public abstract String getHumanReadablePrompt();

  public boolean isConfirmationRequired() {
    return false;
  }

  public boolean isInputRequired() {
    return false;
  }

  public boolean updateFromExchangeIfItMatches(ConformanceExchange exchange) {
    return false;
  }

  public void updateFromPartyInput(JsonNode partyInput) {}
  ;

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
