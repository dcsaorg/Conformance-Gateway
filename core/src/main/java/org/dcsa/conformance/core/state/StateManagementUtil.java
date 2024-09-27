package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public class StateManagementUtil {

    private StateManagementUtil() {}

    public static JsonNode storeMap(Map<String, String> hashMap) {
        return storeMap(hashMap, Function.identity());
    }

    public static <T> JsonNode storeMap(Map<String, T> hashMap, Function<T, String> toString) {
        ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
        hashMap.forEach(
                (key, value) -> {
                    ObjectNode entryNode = OBJECT_MAPPER.createObjectNode();
                    entryNode.put("key", key);
                    entryNode.put("value", toString.apply(value));
                    arrayNode.add(entryNode);
                });
        return arrayNode;
    }

    public static void restoreIntoMap(Map<String, String> map, JsonNode node) {
        restoreIntoMap(map, node, Function.identity());
    }

    public static <T> void restoreIntoMap(Map<String, T> map, JsonNode node, Function<String, T> toValue) {
        StreamSupport.stream(node.spliterator(), false)
                .forEach(
                        entryNode ->
                                map.put(entryNode.get("key").asText(), toValue.apply(entryNode.get("value").asText())));
    }
}
