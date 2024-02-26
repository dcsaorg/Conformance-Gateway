package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Objects;
import java.util.TreeMap;

@Slf4j
public class MemorySortedPartitionsLockingMap extends SortedPartitionsLockingMap {

  private static class MemoryMapItem {
    String lockedBy;
    long lockedUntil;
    JsonNode value;
  }

  private final HashMap<String, TreeMap<String, MemoryMapItem>> memoryMap = new HashMap<>();

  public MemorySortedPartitionsLockingMap() {
    super(60 * 1000, 100, 60 * 1000);
  }

  private MemoryMapItem _getOrCreateItem(String partitionKey, String sortKey) {
    return memoryMap
        .computeIfAbsent(partitionKey, (ignoredKey) -> new TreeMap<>())
        .computeIfAbsent(sortKey, (ignoredKey) -> new MemoryMapItem());
  }

  @Override
  protected void _saveItem(String lockedBy, String partitionKey, String sortKey, JsonNode value) {
    synchronized (memoryMap) {
      MemoryMapItem item = _getOrCreateItem(partitionKey, sortKey);
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
  }

  @Override
  protected JsonNode _loadItem(String lockedBy, String partitionKey, String sortKey)
      throws TemporaryLockingMapException {
    synchronized (memoryMap) {
      MemoryMapItem item = _getOrCreateItem(partitionKey, sortKey);
      long currentTime = System.currentTimeMillis();
      if (item.lockedBy != null && item.lockedUntil > currentTime) {
        log.debug("%s cannot load: must wait for %s to save".formatted(lockedBy, item.lockedBy));
        throw new TemporaryLockingMapException(null);
      } else {
        item.lockedBy = lockedBy;
        item.lockedUntil = currentTime + lockDurationMillis;
      }
      return item.value;
    }
  }

  @Override
  protected void _unlockItem(String lockedBy, String partitionKey, String sortKey) {
    synchronized (memoryMap) {
      MemoryMapItem item = _getOrCreateItem(partitionKey, sortKey);
      if (Objects.equals(lockedBy, item.lockedBy)) {
        if (item.lockedUntil > System.currentTimeMillis()) {
          item.lockedBy = null;
        } else {
          log.debug("%s does not need to unlock: lock has expired".formatted(lockedBy));
        }
      } else {
        throw new RuntimeException("%s cannot unlock: item is locked by %s".formatted(lockedBy, item.lockedBy));
      }
    }
  }
}
