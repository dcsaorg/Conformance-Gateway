package org.dcsa.conformance.core.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.dcsa.conformance.core.state.StatefulEntity;

public class ActionPromptsQueue implements StatefulEntity {
  private final Set<String> allActionIds = new HashSet<>();
  private final LinkedList<JsonNode> pendingActions = new LinkedList<>();

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
    return pendingActions.isEmpty() ? null : pendingActions.removeFirst();
  }

  @Override
  public JsonNode exportJsonState() {
    ArrayNode arrayNode = new ObjectMapper().createArrayNode();
    pendingActions.forEach(arrayNode::add);
    return arrayNode;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    StreamSupport.stream(jsonState.spliterator(), false).forEach(this::addLast);
  }
}
