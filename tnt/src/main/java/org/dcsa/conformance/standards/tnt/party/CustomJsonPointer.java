package org.dcsa.conformance.standards.tnt.party;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;

public class CustomJsonPointer {

  private CustomJsonPointer() {
    // Private constructor to prevent instantiation
  }

  public static List<JsonNode> findMatchingNodes(JsonNode rootNode, String pathExpression, BiPredicate<JsonNode,String> condition, String paramValue) {
    List<JsonNode> results = new ArrayList<>();
    String[] pathSegments = pathExpression.split("/");
    traverse(rootNode, pathSegments, 0, results, condition, paramValue);
    return results;
  }
  /**
   * Recursive function to traverse the JSON tree.
   *
   * @param currentNode The current JSON node being processed.
   * @param pathSegments The segments of the JsonPointer expression.
   * @param segmentIndex The index of the current path segment.
   * @param results The list to store matching JsonNodes.
   * @throws IllegalArgumentException If the path expression is invalid or leads to a non-existent node.
   */
  public static void traverse(JsonNode currentNode, String[] pathSegments, int segmentIndex, List<JsonNode> results, BiPredicate<JsonNode,String> condition, String paramValue) {

    if (segmentIndex == pathSegments.length) {
      addNodeIfConditionMatches(currentNode, results, condition, paramValue);
      return;
    }

    String segment = pathSegments[segmentIndex];

    if (segment.equals("*")) {
      handleWildcardSegment(currentNode, pathSegments, segmentIndex, results, condition, paramValue);
    } else {
      handleRegularSegment(currentNode, pathSegments, segmentIndex, results, condition, paramValue, segment);
    }
  }

  private static void addNodeIfConditionMatches(JsonNode currentNode, List<JsonNode> results, BiPredicate<JsonNode, String> condition, String paramValue) {
    if (condition.test(currentNode, paramValue)) {
      results.add(currentNode);
    }
  }

  private static void handleWildcardSegment(JsonNode currentNode, String[] pathSegments, int segmentIndex, List<JsonNode> results, BiPredicate<JsonNode, String> condition, String paramValue) {
    if (currentNode.isArray()) {
      for (JsonNode element : currentNode) {
        traverse(element, pathSegments, segmentIndex + 1, results, condition, paramValue);
      }
    } else if (currentNode.isObject()) {
      Iterator<String> fieldNames = currentNode.fieldNames();
      while (fieldNames.hasNext()) {
        String fieldName = fieldNames.next();
        traverse(currentNode.get(fieldName), pathSegments, segmentIndex + 1, results, condition, paramValue);
      }
    } else {
      throw new IllegalArgumentException("Invalid wildcard usage in path: " + String.join("/", pathSegments));
    }
  }

  private static void handleRegularSegment(JsonNode currentNode, String[] pathSegments, int segmentIndex, List<JsonNode> results, BiPredicate<JsonNode, String> condition, String paramValue, String segment) {
    JsonNode childNode = currentNode.path(segment);
    if (!childNode.isMissingNode()) {
      traverse(childNode, pathSegments, segmentIndex + 1, results, condition, paramValue);
    }
  }

}
