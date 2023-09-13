package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;

import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

@AllArgsConstructor
public abstract class StatefulExecutor {
  private static final Random RANDOM = new Random();

  private final long loadRetryMillis;
  private final long loadTimeoutMillis;
  protected final long lockDurationMillis;

  public void execute(String partitionKey, String sortKey, Function<JsonNode, JsonNode> function) {
    String lockedBy = UUID.randomUUID().toString();
    JsonNode originalState = _loadOrBlock(lockedBy, partitionKey, sortKey);
    JsonNode modifiedState = function.apply(originalState);
    _saveOrThrow(lockedBy, partitionKey, sortKey, modifiedState);
  }

  private JsonNode _loadOrBlock(String lockedBy, String partitionKey, String sortKey) {
    long timeoutTimestamp = System.currentTimeMillis() + loadTimeoutMillis;
    while (System.currentTimeMillis() < timeoutTimestamp) {
      try {
        return load(lockedBy, partitionKey, sortKey);
      } catch (StatefulExecutorException e) {
        if (StatefulExecutorExceptionCode.ITEM_IS_LOCKED.equals(e.code)) {
          _sleepUpTo(loadRetryMillis);
        } else {
          throw new RuntimeException(
              "Failed to load the state for PK='%s' and SK='%s': %s"
                  .formatted(partitionKey, sortKey, e),
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
      Thread.sleep(RANDOM.nextLong(millis));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void _saveOrThrow(
      String lockedBy, String partitionKey, String sortKey, JsonNode jsonValue) {
    try {
      save(lockedBy, partitionKey, sortKey, jsonValue);
    } catch (StatefulExecutorException e) {
      if (StatefulExecutorExceptionCode.LOCK_HAS_EXPIRED.equals(e.code)) {
        throw new RuntimeException(
            "Found lock expired after %d ms when attempting to save state for PK='%s' and SK='%s'"
                .formatted(lockDurationMillis, partitionKey, sortKey),
            e);
      } else {
        throw new RuntimeException(
            "Failed to save state for PK='%s' and SK='%s': %s".formatted(partitionKey, sortKey, e),
            e);
      }
    }
  }

  protected abstract JsonNode load(String lockedBy, String partitionKey, String sortKey)
      throws StatefulExecutorException;

  protected abstract void save(
      String lockedBy, String partitionKey, String sortKey, JsonNode jsonValue)
      throws StatefulExecutorException;

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
    OTHER_REASON;
  }
}
