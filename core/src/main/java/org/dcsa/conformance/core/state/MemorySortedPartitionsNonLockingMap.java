package org.dcsa.conformance.core.state;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import lombok.SneakyThrows;

public class MemorySortedPartitionsNonLockingMap implements SortedPartitionsNonLockingMap {
  private static final String VALUE = "value";
  private final ConcurrentHashMap<String, ConcurrentSkipListMap<String, JsonNode>> memoryMap = new ConcurrentHashMap<>();

  @SneakyThrows
  @Override
  public synchronized void setItemValue(String partitionKey, String sortKey, JsonNode value) {
    JsonNode valueCopy = OBJECT_MAPPER.readTree(value.toString());
    memoryMap
        .computeIfAbsent(partitionKey, ignoredKey -> new ConcurrentSkipListMap<>())
        .put(sortKey, OBJECT_MAPPER.createObjectNode().set(VALUE, valueCopy));
  }

  @Override
  public JsonNode getItemValue(String partitionKey, String sortKey) {
    return getPartitionOrDefault(partitionKey)
        .getOrDefault(sortKey, OBJECT_MAPPER.createObjectNode())
        .get(VALUE);
  }

  private synchronized ConcurrentSkipListMap<String, JsonNode> getPartitionOrDefault(String partitionKey) {
    return memoryMap.getOrDefault(partitionKey, new ConcurrentSkipListMap<>());
  }

  @Override
  public JsonNode getFirstItemValue(String partitionKey) {
    ConcurrentSkipListMap<String, JsonNode> partition = getPartitionOrDefault(partitionKey);
    return partition.isEmpty() ? null : partition.get(partition.firstKey()).get(VALUE);
  }

  @Override
  public JsonNode getLastItemValue(String partitionKey) {
    ConcurrentSkipListMap<String, JsonNode> partition = getPartitionOrDefault(partitionKey);
    return partition.isEmpty() ? null : partition.get(partition.lastKey()).get(VALUE);
  }

  @Override
  public List<JsonNode> getPartitionValues(String partitionKey) {
    return getPartitionOrDefault(partitionKey).values().stream()
        .map(item -> item.get(VALUE))
        .toList();
  }
}
