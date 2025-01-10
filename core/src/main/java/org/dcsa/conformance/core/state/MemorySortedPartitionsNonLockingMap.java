package org.dcsa.conformance.core.state;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.SneakyThrows;

public class MemorySortedPartitionsNonLockingMap implements SortedPartitionsNonLockingMap {
  private final HashMap<String, TreeMap<String, JsonNode>> memoryMap = new HashMap<>();


  @SneakyThrows
  @Override
  public synchronized void setItemValue(String partitionKey, String sortKey, JsonNode value) {
    JsonNode valueCopy = OBJECT_MAPPER.readTree(value.toString());
    memoryMap
        .computeIfAbsent(partitionKey, ignoredKey -> new TreeMap<>())
        .put(sortKey, OBJECT_MAPPER.createObjectNode().set("value", valueCopy));
  }

  @Override
  public synchronized JsonNode getItemValue(String partitionKey, String sortKey) {
    return memoryMap
        .getOrDefault(partitionKey, new TreeMap<>())
        .getOrDefault(sortKey, OBJECT_MAPPER.createObjectNode())
        .get("value");
  }

  @Override
  public synchronized LinkedHashMap<String, JsonNode> getPartitionValuesBySortKey(
      String partitionKey, String sortKeyPrefix) {
    return memoryMap.getOrDefault(partitionKey, new TreeMap<>()).entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(sortKeyPrefix))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (existing, replacement) -> existing,
                LinkedHashMap::new));
  }
}
