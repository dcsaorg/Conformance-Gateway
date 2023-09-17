package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class MemorySortedPartitionsNonLockingMap implements SortedPartitionsNonLockingMap {
  private final HashMap<String, TreeMap<String, JsonNode>> memoryMap = new HashMap<>();

  @Override
  public synchronized void setItemValue(String partitionKey, String sortKey, JsonNode value) {
    memoryMap
        .computeIfAbsent(partitionKey, (ignoredKey) -> new TreeMap<>())
        .put(sortKey, new ObjectMapper().createObjectNode().set("value", value));
  }

  @Override
  public synchronized JsonNode getItemValue(String partitionKey, String sortKey) {
    return memoryMap
        .getOrDefault(partitionKey, new TreeMap<>())
        .getOrDefault(sortKey, new ObjectMapper().createObjectNode())
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
