package org.dcsa.conformance.gateway.parties;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class ActionPromptsQueue {
  private final Set<String> allActionIds = new HashSet<>();
  private final LinkedList<JsonNode> pendingActions = new LinkedList<>();

  synchronized void addLast(JsonNode actionPrompt) {
    String actionId = actionPrompt.get("actionId").asText();
    if (!allActionIds.contains(actionId)) {
      allActionIds.add(actionId);
      pendingActions.add(actionPrompt);
    }
  }

  synchronized JsonNode removeFirst() {
    return pendingActions.isEmpty() ? null : pendingActions.removeFirst();
  }
}
