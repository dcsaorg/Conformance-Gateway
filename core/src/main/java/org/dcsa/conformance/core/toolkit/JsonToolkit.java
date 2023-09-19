package org.dcsa.conformance.core.toolkit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;

public enum JsonToolkit {
  ; // no instances

  @SneakyThrows
  public static JsonNode stringToJsonNode(String string) {
    return new ObjectMapper().readTree(string);
  }

  @SneakyThrows
  public static JsonNode inputStreamToJsonNode(InputStream inputStream) {
    return new ObjectMapper().readTree(inputStream);
  }

  public static boolean stringAttributeEquals(JsonNode jsonNode, String name, String value) {
    return jsonNode.has(name) && Objects.equals(value, jsonNode.get(name).asText());
  }

  public static String getTextAttributeOrNull(JsonNode jsonNode, String attributeName) {
    JsonNode attributeNode = jsonNode.get(attributeName);
    return attributeNode == null ? null : attributeNode.asText();
  }

  public static ArrayNode stringCollectionToArrayNode(Collection<String> strings) {
    ArrayNode arrayNode = new ObjectMapper().createArrayNode();
    strings.forEach(arrayNode::add);
    return arrayNode;
  }

  public static List<String> arrayNodeToStringCollection(ArrayNode arrayNode) {
    return StreamSupport.stream(arrayNode.spliterator(), false)
        .map(JsonNode::asText)
        .collect(Collectors.toList());
  }
  public static ArrayNode mapOfStringToStringCollectionToJson(Map<String, ? extends Collection<String>> map) {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode queryParamsNode = objectMapper.createArrayNode();
    map.forEach(
            (key, values) -> {
              ObjectNode entryNode = objectMapper.createObjectNode();
              entryNode.put("key", key);
              entryNode.set("values", JsonToolkit.stringCollectionToArrayNode(values));
              queryParamsNode.add(entryNode);
            });
    return queryParamsNode;
  }

  public static Map<String, ? extends Collection<String>> mapOfStringToStringCollectionFromJson(
          ArrayNode arrayNode) {
    HashMap<String, Collection<String>> map = new HashMap<>();
    StreamSupport.stream(arrayNode.spliterator(), false)
            .forEach(
                    entryNode ->
                            map.put(
                                    entryNode.get("key").asText(),
                                    JsonToolkit.arrayNodeToStringCollection((ArrayNode) entryNode.get("values"))));
    return map;
  }
}
