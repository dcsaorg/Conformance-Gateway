package org.dcsa.conformance.core.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.state.StatefulEntity;

import java.util.Collection;
import java.util.LinkedList;

public class ConformanceScenario implements StatefulEntity {
  protected final LinkedList<ConformanceAction> allActions = new LinkedList<>();
  protected final LinkedList<ConformanceAction> nextActions = new LinkedList<>();

  public ConformanceScenario(Collection<ConformanceAction> actions) {
    this.allActions.addAll(actions);
    this.nextActions.addAll(actions);
  }

  @Override
  public JsonNode exportJsonState() {
    ObjectNode jsonState = new ObjectMapper().createObjectNode();
    jsonState.put("nextActionsSize", nextActions.size());

    ArrayNode actionsArrayNode = new ObjectMapper().createArrayNode();
    allActions.forEach(action -> actionsArrayNode.add(action.exportJsonState()));
    jsonState.set("allActions", actionsArrayNode);

    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
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
    return nextActions.pop();
  }

  @Override
  public String toString() {
    return "%s[%s]"
        .formatted(
            getClass().getSimpleName(),
            nextActions.isEmpty() ? "" : nextActions.peekLast().getActionPath());
  }
}
