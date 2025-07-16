package org.dcsa.conformance.core.scenario;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.report.ConformanceStatus;
import org.dcsa.conformance.core.state.StatefulEntity;

@Slf4j
public class ConformanceScenario implements StatefulEntity {
  @Getter private final String title;
  @Getter protected UUID id;
  protected final LinkedList<ConformanceAction> allActions = new LinkedList<>();
  protected final LinkedList<ConformanceAction> nextActions = new LinkedList<>();

  @Getter private ConformanceStatus latestComputedStatus = ConformanceStatus.NO_TRAFFIC;

  public ConformanceScenario(long moduleIndex, long scenarioIndex, Collection<ConformanceAction> actions) {
    this(new UUID(moduleIndex, scenarioIndex), actions);
  }

  public ConformanceScenario(UUID id, Collection<ConformanceAction> actions) {
    this.id = id;
    this.allActions.addAll(actions);
    this.nextActions.addAll(actions);
    this.title = allActions.isEmpty() ? "" : allActions.peekLast().getActionPath();
  }

  public void reset() {
    this.nextActions.clear();
    latestComputedStatus = ConformanceStatus.NO_TRAFFIC;
    allActions.forEach(ConformanceAction::reset);
    this.nextActions.addAll(allActions);
  }

  @Override
  public JsonNode exportJsonState() {
    ObjectNode jsonState = OBJECT_MAPPER.createObjectNode();
    jsonState.put("id", id.toString());

    jsonState.put("nextActionsSize", nextActions.size());

    ArrayNode actionsArrayNode = jsonState.putArray("allActions");
    allActions.forEach(action -> actionsArrayNode.add(action.exportJsonState()));

    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    try {
      if (!jsonState.has("id")) return;
      id = UUID.fromString(jsonState.get("id").asText());

      int nextActionsSize = jsonState.get("nextActionsSize").asInt();
      while (nextActions.size() > nextActionsSize) {
        nextActions.removeFirst();
      }

      ArrayNode allActionsNode = (ArrayNode) jsonState.get("allActions");
      for (int index = 0; index < allActions.size(); ++index) {
        allActions.get(index).importJsonState(allActionsNode.get(index));
      }
    } catch (Exception e) {
      log.warn("Failed to load scenario state: {}", e, e);
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
        "Popped from scenario '%s', current action is now: %s".formatted(toString(), poppedAction.getActionTitle()));
    return poppedAction;
  }

  public String getNextActionsDescription() {
    return nextActions.stream()
        .map(ConformanceAction::getActionTitle)
        .collect(Collectors.joining(" - "));
  }

  public Stream<ConformanceAction> allActionsStream() {
    return allActions.stream();
  }

  @Override
  public String toString() {
    return "%s[ allActions=[%s] nextActions=[%s] ]"
        .formatted(getClass().getSimpleName(), title, getNextActionsDescription());
  }

  public Consumer<ConformanceStatus> computedStatusConsumer() {
    return status -> latestComputedStatus = status;
  }
}
