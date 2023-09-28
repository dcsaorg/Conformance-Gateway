package org.dcsa.conformance.core.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.state.StatefulEntity;

import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class ConformanceScenario implements StatefulEntity {
  @Getter private final String title;
  @Getter protected UUID id = UUID.randomUUID();
  protected final LinkedList<ConformanceAction> allActions = new LinkedList<>();
  protected final LinkedList<ConformanceAction> nextActions = new LinkedList<>();

  public ConformanceScenario(Collection<ConformanceAction> actions) {
    this.allActions.addAll(actions);
    this.nextActions.addAll(actions);
    this.title = allActions.isEmpty() ? "" : allActions.peekLast().getActionPath();
  }

  @Override
  public JsonNode exportJsonState() {
    ObjectNode jsonState = new ObjectMapper().createObjectNode();
    jsonState.put("id", id.toString());

    jsonState.put("nextActionsSize", nextActions.size());

    ArrayNode actionsArrayNode = new ObjectMapper().createArrayNode();
    allActions.forEach(action -> actionsArrayNode.add(action.exportJsonState()));
    jsonState.set("allActions", actionsArrayNode);

    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    id = UUID.fromString(jsonState.get("id").asText());

    int nextActionsSize = jsonState.get("nextActionsSize").asInt();
    while (nextActions.size() > nextActionsSize) {
      nextActions.removeFirst();
    }

    ArrayNode allActionsNode = (ArrayNode) jsonState.get("allActions");
    for (int index = 0; index < allActions.size(); ++index) {
      allActions.get(index).importJsonState(allActionsNode.get(index));
    }
  }

  public boolean hasNextAction() {
    return !nextActions.isEmpty();
  }

  public ConformanceAction peekNextAction() {
    return nextActions.peek();
  }

  public ConformanceAction popNextAction() {
    ConformanceAction poppedAction = nextActions.pop();
    log.info(
        "Popped from scenario '%s' action: %s".formatted(toString(), poppedAction.getActionTitle()));
    return poppedAction;
  }

  public String getNextActionsDescription() {
    return nextActions.stream()
        .map(ConformanceAction::getActionTitle)
        .collect(Collectors.joining(" - "));
  }

  public String getReportTitleDescription() {
    return nextActions.stream()
            .skip(1)
            .map(ConformanceAction::getActionTitle)
            .collect(Collectors.joining(" - "));
  }

  @Override
  public String toString() {
    return "%s[ allActions=[%s] nextActions=[%s] ]"
        .formatted(getClass().getSimpleName(), title, getNextActionsDescription());
  }
}
