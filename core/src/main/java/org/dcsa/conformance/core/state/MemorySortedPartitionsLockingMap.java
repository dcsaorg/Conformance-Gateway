package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemorySortedPartitionsLockingMap extends SortedPartitionsLockingMap {

  private static class MemoryMapItem {
    String lockedBy;
    long lockedUntil;
    JsonNode value;
  }

  private final Map<String, Map<String, MemoryMapItem>> memoryMap = new ConcurrentHashMap<>();

  public MemorySortedPartitionsLockingMap() {
    super(60L * 1000L, 100L, 60L * 1000L);
  }

  private MemoryMapItem getOrCreateItem(String partitionKey, String sortKey) {
    return memoryMap
        .computeIfAbsent(partitionKey, ignoredKey -> new ConcurrentSkipListMap<>())
        .computeIfAbsent(sortKey, ignoredKey -> new MemoryMapItem());
  }

  @Override
  protected void _saveItem(String lockedBy, String partitionKey, String sortKey, JsonNode value) {
      MemoryMapItem item = getOrCreateItem(partitionKey, sortKey);
      if (Objects.equals(lockedBy, item.lockedBy)) {
        if (item.lockedUntil > System.currentTimeMillis()) {
          item.value = value;
          item.lockedBy = null;
        } else {
          throw new RuntimeException("%s cannot save: lock has expired".formatted(lockedBy));
        }
      } else {
        throw new RuntimeException("%s cannot save: item is locked by %s".formatted(lockedBy, item.lockedBy));
      }
  }

  @Override
  protected JsonNode _loadItem(String lockedBy, String partitionKey, String sortKey)
      throws TemporaryLockingMapException {
      MemoryMapItem item = getOrCreateItem(partitionKey, sortKey);
      long currentTime = System.currentTimeMillis();
      if (item.lockedBy != null && item.lockedUntil > currentTime) {
      log.debug("{} cannot load: must wait for {} to save", lockedBy, item.lockedBy);
        throw new TemporaryLockingMapException(null);
      } else {
        item.lockedBy = lockedBy;
        item.lockedUntil = currentTime + lockDurationMillis;
      }
      return item.value;
  }

  @Override
  protected void _unlockItem(String lockedBy, String partitionKey, String sortKey) {
      MemoryMapItem item = getOrCreateItem(partitionKey, sortKey);
      if (Objects.equals(lockedBy, item.lockedBy)) {
        if (item.lockedUntil > System.currentTimeMillis()) {
          item.lockedBy = null;
        } else {
          log.debug("{} does not need to unlock: lock has expired", lockedBy);
        }
      } else {
        throw new RuntimeException("%s cannot unlock: item is locked by %s".formatted(lockedBy, item.lockedBy));
      }
    }
}
