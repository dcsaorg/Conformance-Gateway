package org.dcsa.conformance.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Objects;
import java.util.TreeMap;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.state.StatefulExecutor;

@Slf4j
public class MemoryMapStatefulExecutor extends StatefulExecutor {
  private final HashMap<String, TreeMap<String, MemoryMapItem>> memoryMap = new HashMap<>();

  public MemoryMapStatefulExecutor() {
    super(500, 5 * 1000, 5 * 1000);
  }

  @Override
  protected JsonNode load(String lockedBy, String partitionKey, String sortKey)
      throws StatefulExecutorException {
    synchronized (memoryMap) {
      MemoryMapItem item = _getOrCreateItem(partitionKey, sortKey);
      long currentTime = System.currentTimeMillis();
      if (item.lockedBy != null && item.lockedUntil > currentTime) {
        log.debug("%s cannot load: must wait for %s to save".formatted(lockedBy, item.lockedBy));
        throw new StatefulExecutorException(StatefulExecutorExceptionCode.ITEM_IS_LOCKED, null);
      } else {
        item.lockedBy = lockedBy;
        item.lockedUntil = currentTime + lockDurationMillis;
      }
      return item.value;
    }
  }

  @Override
  protected void save(String lockedBy, String partitionKey, String sortKey, JsonNode jsonValue)
      throws StatefulExecutorException {
    synchronized (memoryMap) {
      MemoryMapItem item = _getOrCreateItem(partitionKey, sortKey);
      if (Objects.equals(lockedBy, item.lockedBy)) {
        if (item.lockedUntil > System.currentTimeMillis()) {
          item.value = jsonValue;
          item.lockedBy = null;
        } else {
          log.debug("%s cannot save: lock has expired".formatted(lockedBy));
          throw new StatefulExecutorException(StatefulExecutorExceptionCode.LOCK_HAS_EXPIRED, null);
        }
      } else {
        log.debug("%s cannot save: item is locked by %s".formatted(lockedBy, item.lockedBy));
        throw new StatefulExecutorException(StatefulExecutorExceptionCode.ITEM_NOT_LOCKED, null);
      }
    }
  }

  @Override
  protected void unlock(String lockedBy, String partitionKey, String sortKey) {
    synchronized (memoryMap) {
      MemoryMapItem item = _getOrCreateItem(partitionKey, sortKey);
      if (Objects.equals(lockedBy, item.lockedBy)) {
        if (item.lockedUntil > System.currentTimeMillis()) {
          item.lockedBy = null;
        } else {
          log.debug("%s does not need to unlock: lock has expired".formatted(lockedBy));
        }
      } else {
        log.debug("%s cannot unlock: item is locked by %s".formatted(lockedBy, item.lockedBy));
      }
    }
  }

  private MemoryMapItem _getOrCreateItem(String partitionKey, String sortKey) {
    return memoryMap
        .computeIfAbsent(partitionKey, (ignoredKey) -> new TreeMap<>())
        .computeIfAbsent(sortKey, (ignoredKey) -> new MemoryMapItem());
  }

  private static class MemoryMapItem {
    private String lockedBy;
    private long lockedUntil;
    private JsonNode value;
  }
}
