package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public abstract class SortedPartitionsLockingMap {
  private static final Random RANDOM = new Random();

  protected final long lockDurationMillis;
  private final long loadRetryMillis;
  private final long loadTimeoutMillis;

  protected SortedPartitionsLockingMap(
      long lockDurationMillis, long loadRetryMillis, long loadTimeoutMillis) {
    this.lockDurationMillis = lockDurationMillis;
    this.loadRetryMillis = loadRetryMillis;
    this.loadTimeoutMillis = loadTimeoutMillis;
  }

  public JsonNode loadItem(String lockedBy, String partitionKey, String sortKey) {
    log.debug(
        "SortedPartitionsLockingMap.loadItem(lockedBy='%s', partitionKey='%s', sortKey='%s') starting..."
            .formatted(lockedBy, partitionKey, sortKey));
    long timeoutTimestamp = System.currentTimeMillis() + loadTimeoutMillis;
    while (System.currentTimeMillis() < timeoutTimestamp) {
      try {
        JsonNode loadedItem = _loadItem(lockedBy, partitionKey, sortKey);
        log.debug(
            "SortedPartitionsLockingMap.loadItem(lockedBy='%s', partitionKey='%s', sortKey='%s') DONE"
                .formatted(lockedBy, partitionKey, sortKey));
        return loadedItem;
      } catch (TemporaryLockingMapException e) {
        _sleepUpTo(loadRetryMillis);
      }
    }
    throw new RuntimeException(
        "Timed out after %d ms attempting to load item with PK='%s' and SK='%s'"
            .formatted(loadTimeoutMillis, partitionKey, sortKey));
  }

  private void _sleepUpTo(long millis) {
    try {
      Thread.sleep(RANDOM.nextLong(millis / 2, millis));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void saveItem(String lockedBy, String partitionKey, String sortKey, JsonNode value) {
    log.debug(
        "SortedPartitionsLockingMap.saveItem(lockedBy='%s', partitionKey='%s', sortKey='%s', ...) starting..."
            .formatted(lockedBy, partitionKey, sortKey));
    _saveItem(lockedBy, partitionKey, sortKey, value);
    log.debug(
        "SortedPartitionsLockingMap.saveItem(lockedBy='%s', partitionKey='%s', sortKey='%s', ...) DONE"
            .formatted(lockedBy, partitionKey, sortKey));
  }

  public void unlockItem(String lockedBy, String partitionKey, String sortKey) {
    log.debug(
        "SortedPartitionsLockingMap.unlockItem(lockedBy='%s', partitionKey='%s', sortKey='%s', ...) starting..."
            .formatted(lockedBy, partitionKey, sortKey));
    _unlockItem(lockedBy, partitionKey, sortKey);
    log.debug(
        "SortedPartitionsLockingMap.unlockItem(lockedBy='%s', partitionKey='%s', sortKey='%s', ...) DONE"
            .formatted(lockedBy, partitionKey, sortKey));
  }

  protected abstract void _saveItem(
      String lockedBy, String partitionKey, String sortKey, JsonNode value);

  protected abstract JsonNode _loadItem(String lockedBy, String partitionKey, String sortKey)
      throws TemporaryLockingMapException;

  protected abstract void _unlockItem(String lockedBy, String partitionKey, String sortKey);
}
