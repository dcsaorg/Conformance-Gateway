package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

@AllArgsConstructor
@Slf4j
public abstract class StatefulExecutor {
  private static final Random RANDOM = new Random();

  private final long loadRetryMillis;
  private final long loadTimeoutMillis;
  protected final long lockDurationMillis;

  public void execute(
      String description,
      String partitionKey,
      String sortKey,
      Function<JsonNode, JsonNode> function) {
    String lockedBy = UUID.randomUUID().toString();
    log.debug("Executing with lock %s: %s".formatted(lockedBy, description));
    JsonNode originalState = _loadOrBlock(lockedBy, partitionKey, sortKey);
    JsonNode modifiedState;
    try {
      modifiedState = function.apply(originalState);
    } catch (Throwable t) {
      log.error(
          "Execution failed, unlocking (lockedBy='%s', partitionKey='%s', sortKey='%s'): %s"
              .formatted(lockedBy, partitionKey, sortKey, t),
          t);
      unlock(lockedBy, partitionKey, sortKey);
      throw new RuntimeException("Execution failed: " + t, t);
    }
    _saveOrThrow(lockedBy, partitionKey, sortKey, modifiedState);
  }

  private JsonNode _loadOrBlock(String lockedBy, String partitionKey, String sortKey) {
    log.debug(
        "StatefulExecutor._loadOrBlock(lockedBy='%s', partitionKey='%s', sortKey='%s') starting..."
            .formatted(lockedBy, partitionKey, sortKey));
    long timeoutTimestamp = System.currentTimeMillis() + loadTimeoutMillis;
    while (System.currentTimeMillis() < timeoutTimestamp) {
      try {
        JsonNode loadedState = load(lockedBy, partitionKey, sortKey);
        log.debug(
            "StatefulExecutor._loadOrBlock(lockedBy='%s', partitionKey='%s', sortKey='%s') DONE"
                .formatted(lockedBy, partitionKey, sortKey));
        return loadedState;
      } catch (StatefulExecutorException e) {
        if (StatefulExecutorExceptionCode.ITEM_IS_LOCKED.equals(e.code)) {
          _sleepUpTo(loadRetryMillis);
        } else {
          throw new RuntimeException(
              "Failed to load the state for lockedBy='%s', partitionKey='%s', sortKey='%s': %s"
                  .formatted(lockedBy, partitionKey, sortKey, e),
              e);
        }
      }
    }
    throw new RuntimeException(
        "Timed out after %d ms attempting to load the state for PK='%s' and SK='%s'"
            .formatted(loadTimeoutMillis, partitionKey, sortKey));
  }

  private void _sleepUpTo(long millis) {
    try {
      Thread.sleep(RANDOM.nextLong(millis / 2, millis));
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while sleeping during attempts to load", e);
    }
  }

  private void _saveOrThrow(
      String lockedBy, String partitionKey, String sortKey, JsonNode jsonValue) {
    try {
      log.debug(
          "StatefulExecutor._saveOrThrow(lockedBy='%s', partitionKey='%s', sortKey='%s', ...) starting..."
              .formatted(lockedBy, partitionKey, sortKey));
      save(lockedBy, partitionKey, sortKey, jsonValue);
      log.debug(
          "StatefulExecutor._saveOrThrow(lockedBy='%s', partitionKey='%s', sortKey='%s', ...) DONE"
              .formatted(lockedBy, partitionKey, sortKey));
    } catch (StatefulExecutorException e) {
      if (StatefulExecutorExceptionCode.LOCK_HAS_EXPIRED.equals(e.code)) {
        throw new RuntimeException(
            "Found lock expired after %d ms when attempting to save state for lockedBy='%s', partitionKey='%s', sortKey='%s'"
                .formatted(lockDurationMillis, lockedBy, partitionKey, sortKey),
            e);
      } else {
        throw new RuntimeException(
            "Failed to save state for lockedBy='%s', partitionKey='%s', sortKey='%s': %s"
                .formatted(lockedBy, partitionKey, sortKey, e),
            e);
      }
    }
  }

  protected abstract JsonNode load(String lockedBy, String partitionKey, String sortKey)
      throws StatefulExecutorException;

  protected abstract void save(
      String lockedBy, String partitionKey, String sortKey, JsonNode jsonValue)
      throws StatefulExecutorException;

  protected abstract void unlock(String lockedBy, String partitionKey, String sortKey);

  protected static class StatefulExecutorException extends Exception {
    protected final StatefulExecutorExceptionCode code;

    public StatefulExecutorException(StatefulExecutorExceptionCode code, Throwable cause) {
      super(
          "%s: %s".formatted(StatefulExecutorException.class.getSimpleName(), code.name()), cause);
      this.code = code;
    }
  }

  protected enum StatefulExecutorExceptionCode {
    ITEM_IS_LOCKED,
    ITEM_NOT_LOCKED,
    LOCK_HAS_EXPIRED,
    // OTHER_REASON;
  }
}
