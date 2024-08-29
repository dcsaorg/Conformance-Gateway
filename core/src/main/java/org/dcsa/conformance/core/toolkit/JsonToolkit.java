package org.dcsa.conformance.core.toolkit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;

public enum JsonToolkit {
  ; // no instances

  public static final String JSON_UTF_8 = "application/json";
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @SneakyThrows
  public static JsonNode stringToJsonNode(String string) {
    return OBJECT_MAPPER.readTree(string);
  }

  @SneakyThrows
  public static JsonNode inputStreamToJsonNode(InputStream inputStream) {
    return OBJECT_MAPPER.readTree(inputStream);
  }

  @SneakyThrows
  public static JsonNode templateFileToJsonNode(
      String templatePath, Map<String, String> replacements) {
    AtomicReference<String> jsonString = new AtomicReference<>();
    try (InputStream inputStream = JsonToolkit.class.getResourceAsStream(templatePath)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Could not resolve " + templatePath);
      }
      jsonString.set(
          new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
    }
    replacements.forEach((key, value) -> jsonString.set(jsonString.get().replaceAll(key, value)));
    return OBJECT_MAPPER.readTree(jsonString.get());
  }

  public static ArrayNode stringCollectionToArrayNode(Collection<String> strings) {
    ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
    strings.forEach(arrayNode::add);
    return arrayNode;
  }

  public static List<String> arrayNodeToStringCollection(ArrayNode arrayNode) {
    return StreamSupport.stream(arrayNode.spliterator(), false).map(JsonNode::asText).toList();
  }

  public static ArrayNode mapOfStringToStringCollectionToJson(
      Map<String, ? extends Collection<String>> map) {
    ArrayNode queryParamsNode = OBJECT_MAPPER.createArrayNode();
    map.forEach(
        (key, values) -> {
          queryParamsNode.addObject()
            .put("key", key)
            .set("values", JsonToolkit.stringCollectionToArrayNode(values));
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
