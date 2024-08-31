package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.UserFacingException;

@Slf4j
public class StatefulExecutor {
  private static final AtomicInteger maxStateLength = new AtomicInteger(0);

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
      if (log.isInfoEnabled()) {
        int stateLength = modifiedState.toString().getBytes(StandardCharsets.UTF_8).length;
        log.info(
            "Max saved state length is now: %d"
                .formatted(maxStateLength.accumulateAndGet(stateLength, Math::max)));
        if (stateLength > 100 * 1000) {
          log.error(
              "A saved state of size '%d' is too large: %s"
                  .formatted(stateLength, modifiedState.toPrettyString()));
          throw new IllegalArgumentException("Saved state is too large");
        }
      }
    } else {
      sortedPartitionsLockingMap.unlockItem(lockedBy, partitionKey, sortKey);
    }
  }
}
