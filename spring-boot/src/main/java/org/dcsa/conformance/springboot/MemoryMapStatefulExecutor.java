package org.dcsa.conformance.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Objects;
import java.util.TreeMap;
import org.dcsa.conformance.core.state.StatefulExecutor;

public class MemoryMapStatefulExecutor extends StatefulExecutor {
  private final HashMap<String, TreeMap<String, MemoryMapItem>> memoryMap = new HashMap<>();

  private final Object privateLock = new Object();

  public MemoryMapStatefulExecutor() {
    super(100, 1000, 5000);
  }

  @Override
  protected JsonNode load(String lockedBy, String partitionKey, String sortKey)
      throws StatefulExecutorException {
    synchronized (privateLock) {
      MemoryMapItem item = _getOrCreateItem(partitionKey, sortKey);
      long currentTime = System.currentTimeMillis();
      if (item.lockedBy != null && item.lockedUntil > currentTime) {
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
    synchronized (privateLock) {
      MemoryMapItem item = _getOrCreateItem(partitionKey, sortKey);
      if (Objects.equals(lockedBy, item.lockedBy)) {
        if (item.lockedUntil > System.currentTimeMillis()) {
          item.value = jsonValue;
          item.lockedBy = null;
        } else {
          throw new StatefulExecutorException(StatefulExecutorExceptionCode.LOCK_HAS_EXPIRED, null);
        }
      } else {
        throw new StatefulExecutorException(StatefulExecutorExceptionCode.ITEM_NOT_LOCKED, null);
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
