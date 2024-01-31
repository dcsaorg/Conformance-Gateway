package org.dcsa.conformance.core.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.state.StatefulEntity;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public class ActionPromptsQueue implements StatefulEntity {
  private final Set<String> allActionIds = new HashSet<>();
  private final LinkedList<JsonNode> pendingActions = new LinkedList<>();

  synchronized void clear() {
    allActionIds.clear();
    pendingActions.clear();
  }

  synchronized void addLast(JsonNode actionPrompt) {
    String actionId = actionPrompt.get("actionId").asText();
    if (!allActionIds.contains(actionId)) {
      allActionIds.add(actionId);
      pendingActions.add(actionPrompt);
    }
  }

  synchronized boolean isEmpty() {
    return pendingActions.isEmpty();
  }

  synchronized JsonNode removeFirst() {
    if (pendingActions.isEmpty()) {
      return null;
    }
    JsonNode actionPrompt = pendingActions.removeFirst();
    allActionIds.remove(actionPrompt.get("actionId").asText());
    return actionPrompt;
  }

  @Override
  synchronized public JsonNode exportJsonState() {
    ObjectNode stateNode = OBJECT_MAPPER.createObjectNode();

    ArrayNode allActionIdsNode = stateNode.putArray("allActionIds");
    allActionIds.forEach(allActionIdsNode::add);

    ArrayNode pendingActionsNode = stateNode.putArray("pendingActions");
    pendingActions.forEach(pendingActionsNode::add);
    stateNode.set("pendingActions", pendingActionsNode);

    return stateNode;
  }

  @Override
  synchronized public void importJsonState(JsonNode jsonState) {
    StreamSupport.stream(jsonState.get("allActionIds").spliterator(), false)
            .map(JsonNode::asText)
            .forEach(allActionIds::add);
    StreamSupport.stream(jsonState.get("pendingActions").spliterator(), false)
        .forEach(pendingActions::add);
  }
}
