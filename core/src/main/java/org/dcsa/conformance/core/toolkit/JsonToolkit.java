package org.dcsa.conformance.core.toolkit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.StreamSupport;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.dcsa.conformance.core.util.JsonUtil;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class JsonToolkit {

  public static final String JSON_UTF_8 = "application/json";
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  public static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  public static final DateTimeFormatter ISO_8601_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  @SneakyThrows
  public static JsonNode stringToJsonNode(String string) {
    return OBJECT_MAPPER.readTree(string);
  }

  @SneakyThrows
  public static JsonNode inputStreamToJsonNode(InputStream inputStream) {
    return OBJECT_MAPPER.readTree(inputStream);
  }

  @SneakyThrows
  public static void writeJsonNodeToOutputStream(JsonNode jsonNode, OutputStream outputStream) {
    try (BufferedWriter writer =
        new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
      writer.write(jsonNode.toPrettyString());
      writer.flush();
    }
  }

  @SneakyThrows
  public static JsonNode templateFileToJsonNode(
      String templatePath, Map<String, String> replacements) {
    JsonNode jsonNode =
        OBJECT_MAPPER.readTree(IOToolkit.templateFileToText(templatePath, replacements));
    removeEmptyOrNullFields(jsonNode);
    return jsonNode;
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
        (key, values) -> queryParamsNode.addObject()
          .put("key", key)
          .set("values", JsonToolkit.stringCollectionToArrayNode(values)));
    return queryParamsNode;
  }

  public static Map<String, ? extends Collection<String>> mapOfStringToStringCollectionFromJson(
      ArrayNode arrayNode) {
    Map<String, Collection<String>> map = new HashMap<>();
    StreamSupport.stream(arrayNode.spliterator(), false)
        .forEach(
            entryNode ->
                map.put(
                    entryNode.get("key").asText(),
                    JsonToolkit.arrayNodeToStringCollection((ArrayNode) entryNode.get("values"))));
    return map;
  }

  private static void removeEmptyOrNullFields(JsonNode node) {
    if (node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      List<String> fieldsToRemove = new ArrayList<>();

      Iterator<String> fieldNames = objectNode.fieldNames();
      while (fieldNames.hasNext()) {
        String fieldName = fieldNames.next();
        JsonNode value = objectNode.get(fieldName);

        if (JsonUtil.isMissingOrEmpty(value)) {
          fieldsToRemove.add(fieldName);
        } else if (value.isContainerNode()) {
          removeEmptyOrNullFields(value);
        }
      }
      fieldsToRemove.forEach(objectNode::remove);
    } else if (node.isArray()) {
      node.forEach(JsonToolkit::removeEmptyOrNullFields);
    }
  }
}
