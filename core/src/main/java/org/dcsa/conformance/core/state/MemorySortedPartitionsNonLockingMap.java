package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public class MemorySortedPartitionsNonLockingMap implements SortedPartitionsNonLockingMap {
  private final HashMap<String, TreeMap<String, JsonNode>> memoryMap = new HashMap<>();


  @SneakyThrows
  @Override
  public synchronized void setItemValue(String partitionKey, String sortKey, JsonNode value) {
    JsonNode valueCopy = OBJECT_MAPPER.readTree(value.toString());
    memoryMap
        .computeIfAbsent(partitionKey, (ignoredKey) -> new TreeMap<>())
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
  public synchronized JsonNode getFirstItemValue(String partitionKey) {
    TreeMap<String, JsonNode> partition = memoryMap.getOrDefault(partitionKey, new TreeMap<>());
    return partition.isEmpty() ? null : partition.get(partition.firstKey()).get("value");
  }

  @Override
  public synchronized JsonNode getLastItemValue(String partitionKey) {
    TreeMap<String, JsonNode> partition = memoryMap.getOrDefault(partitionKey, new TreeMap<>());
    return partition.isEmpty() ? null : partition.get(partition.lastKey()).get("value");
  }

  @Override
  public synchronized List<JsonNode> getPartitionValues(String partitionKey) {
    return memoryMap.getOrDefault(partitionKey, new TreeMap<>()).values().stream()
        .map(item -> item.get("value"))
        .toList();
  }
}
