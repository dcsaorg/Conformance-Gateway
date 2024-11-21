package org.dcsa.conformance.standards.tnt.party;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Set;

@UtilityClass
public class TntHelper {

  private boolean isEventNode(JsonNode jsonNode) {
    return jsonNode.isObject() && !jsonNode.path("eventType").isMissingNode();
  }

  public ArrayList<JsonNode> findEventNodes(JsonNode jsonResponse, Set<String> issues) {
    ArrayList<JsonNode> eventNodes = new ArrayList<>();

    try {
      if (jsonResponse.isArray()) {
        // Handle top-level array
        jsonResponse.forEach(elementNode -> findEventNodes(eventNodes, elementNode));

      } else if (jsonResponse.isObject() && jsonResponse.has("events")) {
        // Handle object with "events" array
        JsonNode eventsArray = jsonResponse.path("events");
        if (eventsArray.isArray()) {
          eventsArray.forEach(elementNode -> findEventNodes(eventNodes, elementNode));
        } else {
          throw new IllegalStateException("Events field exists but is not an array");
        }
      } else {
        throw new IllegalStateException("Invalid JSON data structure: exists");
      }
    } catch (IllegalStateException e) {
      issues.add(e.getMessage());
    }
    return eventNodes;
  }

  private void findEventNodes(ArrayList<JsonNode> foundEventNodes, JsonNode searchInJsonNode) {
    if (isEventNode(searchInJsonNode)) {
      foundEventNodes.add(searchInJsonNode);
    } else { // Recursive call to check children
      searchInJsonNode.forEach(elementNode -> findEventNodes(foundEventNodes, elementNode));
    }
  }
}
