package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.UserFacingException;

@Slf4j
public class StatefulExecutor {
  private final SortedPartitionsLockingMap sortedPartitionsLockingMap;

  public StatefulExecutor(SortedPartitionsLockingMap sortedPartitionsLockingMap) {
    this.sortedPartitionsLockingMap = sortedPartitionsLockingMap;
  }

  public void execute(
      String description,
      String partitionKey,
      String sortKey,
      Function<JsonNode, JsonNode> function) {
    String lockedBy = UUID.randomUUID().toString();
    log.info("Executing with lock %s: %s".formatted(lockedBy, description));
    JsonNode originalState = sortedPartitionsLockingMap.loadItem(lockedBy, partitionKey, sortKey);

    JsonNode modifiedState;
    try {
      modifiedState = function.apply(originalState);
    } catch (Throwable t) {
      log.warn(
          "Execution failed, unlocking (lockedBy='%s', partitionKey='%s', sortKey='%s'): %s"
              .formatted(lockedBy, partitionKey, sortKey, t),
          t);
      sortedPartitionsLockingMap.unlockItem(lockedBy, partitionKey, sortKey);
      if (t instanceof UserFacingException) {
        throw t;
      }
      throw new RuntimeException("Execution failed: " + t, t);
    }

    if (modifiedState != null) {
      sortedPartitionsLockingMap.saveItem(lockedBy, partitionKey, sortKey, modifiedState);
    } else {
      sortedPartitionsLockingMap.unlockItem(lockedBy, partitionKey, sortKey);
    }
  }
}
