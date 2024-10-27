package org.dcsa.conformance.sandbox.state;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.state.SortedPartitionsLockingMap;
import org.dcsa.conformance.core.state.TemporaryLockingMapException;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Slf4j
public class DynamoDbSortedPartitionsLockingMap extends SortedPartitionsLockingMap {

  private static final int MAX_TRANSACTION_RETRY_COUNT = 5;

  private final DynamoDbClient dynamoDbClient;
  private final String tableName;

  public DynamoDbSortedPartitionsLockingMap(DynamoDbClient dynamoDbClient, String tableName) {
    super(60L * 1000L, 500L, 60L * 1000L);
    this.dynamoDbClient = dynamoDbClient;
    this.tableName = tableName;
  }

  @Override
  protected void _saveItem(String lockedBy, String partitionKey, String sortKey, JsonNode value) {
    log.info(
        "START _saveItem(LB=%s, PK=%s, SK=%s, ...)".formatted(lockedBy, partitionKey, sortKey));
    String oldLockedUntil = Instant.now().toString();
    String newLockedUntil = Instant.ofEpochMilli(Instant.now().toEpochMilli() - 1).toString();
    Map<String, AttributeValue> key =
        Map.ofEntries(
            Map.entry("PK", AttributeValue.fromS(partitionKey)),
            Map.entry("SK", AttributeValue.fromS(sortKey)));
    try {
      _retryTransactWriteItems(
          TransactWriteItemsRequest.builder()
              .transactItems(
                  TransactWriteItem.builder()
                      .update(
                          Update.builder()
                              .tableName(tableName)
                              .key(key)
                              .conditionExpression(
                                  "attribute_not_exists(PK) "
                                      + "OR (lockedUntil < :olu) "
                                      + "OR ("
                                      + "attribute_exists(lockedBy) "
                                      + "AND lockedBy = :lb "
                                      + "AND lockedUntil > :olu"
                                      + ")")
                              .updateExpression("SET #v = :v, #lb = :lb, #lu = :nlu")
                              .expressionAttributeNames(
                                  Map.ofEntries(
                                      Map.entry("#v", "value"),
                                      Map.entry("#lb", "lockedBy"),
                                      Map.entry("#lu", "lockedUntil")))
                              .expressionAttributeValues(
                                  Map.ofEntries(
                                      Map.entry(":v", AttributeValue.fromS(value.toString())),
                                      Map.entry(":lb", AttributeValue.fromS(lockedBy)),
                                      Map.entry(":olu", AttributeValue.fromS(oldLockedUntil)),
                                      Map.entry(":nlu", AttributeValue.fromS(newLockedUntil))))
                              .build())
                      .build())
              .build());
    } catch (TransactionCanceledException transactionCanceledException) {
      log.info(
          "CATCH _saveItem(LB=%s, PK=%s, SK=%s, ...)".formatted(lockedBy, partitionKey, sortKey));
      log.warn(
          "%s: %s"
              .formatted(
                  transactionCanceledException,
                  transactionCanceledException.cancellationReasons().stream()
                      .map(reason -> "%s %s".formatted(reason.code(), reason.message()))
                      .collect(Collectors.joining(", "))));
      throw new RuntimeException(transactionCanceledException);
    }
    log.info("END _saveItem(LB=%s, PK=%s, SK=%s, ...)".formatted(lockedBy, partitionKey, sortKey));
  }

  @Override
  protected JsonNode _loadItem(String lockedBy, String partitionKey, String sortKey)
      throws TemporaryLockingMapException {
    log.info(
        "START _loadItem(LB=%s, PK=%s, SK=%s, ...)".formatted(lockedBy, partitionKey, sortKey));
    Map<String, AttributeValue> key =
        Map.ofEntries(
            Map.entry("PK", AttributeValue.fromS(partitionKey)),
            Map.entry("SK", AttributeValue.fromS(sortKey)));

    _createOrLockItem(lockedBy, partitionKey, sortKey, key);

    log.info(
      "END _loadItem(LB=%s, PK=%s, SK=%s, ...)".formatted(lockedBy, partitionKey, sortKey));
    return _loadLockedItem(lockedBy, partitionKey, sortKey, key);
  }

  private void _createOrLockItem(
      String lockedBy, String partitionKey, String sortKey, Map<String, AttributeValue> key)
      throws TemporaryLockingMapException {
    log.info(
        "START _createOrLockItem(LB=%s, PK=%s, SK=%s, ...)"
            .formatted(lockedBy, partitionKey, sortKey));
    try {
      String lockedUntil =
          Instant.ofEpochMilli(Instant.now().toEpochMilli() + lockDurationMillis).toString();
      _retryTransactWriteItems(
          TransactWriteItemsRequest.builder()
              .transactItems(
                  TransactWriteItem.builder()
                      .put(
                          Put.builder()
                              .tableName(tableName)
                              .conditionExpression("attribute_not_exists(PK)")
                              .item(
                                  Map.ofEntries(
                                      Map.entry("PK", AttributeValue.fromS(partitionKey)),
                                      Map.entry("SK", AttributeValue.fromS(sortKey)),
                                      Map.entry("lockedBy", AttributeValue.fromS(lockedBy)),
                                      Map.entry("lockedUntil", AttributeValue.fromS(lockedUntil))))
                              .build())
                      .build())
              .build());
    } catch (TransactionCanceledException transactionCanceledException) {
      if ("ConditionalCheckFailed"
          .equals(transactionCanceledException.cancellationReasons().getFirst().code())) {
        log.info(
            "ConditionalCheckFailed _createOrLockItem(LB=%s, PK=%s, SK=%s, ...)"
                .formatted(lockedBy, partitionKey, sortKey));
        _lockExistingItem(lockedBy, partitionKey, sortKey, key);
      } else {
        log.info(
            "TransactionCanceled _createOrLockItem(LB=%s, PK=%s, SK=%s, ...)"
                .formatted(lockedBy, partitionKey, sortKey));
        log.warn(
            "Failed to create non-existing item (exception='%s' reasons='%s')"
                .formatted(
                    transactionCanceledException,
                    transactionCanceledException.cancellationReasons().stream()
                        .map(reason -> "%s %s".formatted(reason.code(), reason.message()))
                        .collect(Collectors.joining(", "))));
        throw transactionCanceledException;
      }
    }
    log.info(
        "END _createOrLockItem(LB=%s, PK=%s, SK=%s, ...)"
            .formatted(lockedBy, partitionKey, sortKey));
  }

  private void _lockExistingItem(
      String lockedBy, String partitionKey, String sortKey, Map<String, AttributeValue> key)
      throws TemporaryLockingMapException {
    log.info(
        "START _lockExistingItem(LB=%s, PK=%s, SK=%s, ...)"
            .formatted(lockedBy, partitionKey, sortKey));
    String oldLockedUntil = Instant.now().toString();
    String newLockedUntil =
        Instant.ofEpochMilli(Instant.now().toEpochMilli() + lockDurationMillis).toString();
    try {
      _retryTransactWriteItems(
          TransactWriteItemsRequest.builder()
              .transactItems(
                  TransactWriteItem.builder()
                      .update(
                          Update.builder()
                              .tableName(tableName)
                              .key(key)
                              .conditionExpression(
                                  "attribute_not_exists(lockedUntil) OR #lu < :olu")
                              .updateExpression("SET #lb = :lb, #lu = :nlu")
                              .expressionAttributeNames(
                                  Map.ofEntries(
                                      Map.entry("#lb", "lockedBy"),
                                      Map.entry("#lu", "lockedUntil")))
                              .expressionAttributeValues(
                                  Map.ofEntries(
                                      Map.entry(":lb", AttributeValue.fromS(lockedBy)),
                                      Map.entry(":olu", AttributeValue.fromS(oldLockedUntil)),
                                      Map.entry(":nlu", AttributeValue.fromS(newLockedUntil))))
                              .build())
                      .build())
              .build());
    } catch (TransactionCanceledException transactionCanceledException) {
      if ("ConditionalCheckFailed"
          .equals(transactionCanceledException.cancellationReasons().getFirst().code())) {
        log.warn(
            "ConditionalCheckFailed _lockExistingItem(LB=%s, PK=%s, SK=%s, ...)"
                .formatted(lockedBy, partitionKey, sortKey));
        throw new TemporaryLockingMapException(transactionCanceledException);
      } else {
        log.info(
            "TransactionCanceled _lockExistingItem(LB=%s, PK=%s, SK=%s, ...)"
                .formatted(lockedBy, partitionKey, sortKey));
        log.warn(
            "Failed to lock existing item (exception='%s' reasons='%s')"
                .formatted(
                    transactionCanceledException,
                    transactionCanceledException.cancellationReasons().stream()
                        .map(reason -> "%s %s".formatted(reason.code(), reason.message()))
                        .collect(Collectors.joining(", "))));
        throw transactionCanceledException;
      }
    }
    log.info(
        "END _lockExistingItem(LB=%s, PK=%s, SK=%s, ...)"
            .formatted(lockedBy, partitionKey, sortKey));
  }

  private JsonNode _loadLockedItem(
      String lockedBy, String partitionKey, String sortKey, Map<String, AttributeValue> key) {
    log.info(
        "START _loadLockedItem(LB=%s, PK=%s, SK=%s, ...)"
            .formatted(lockedBy, partitionKey, sortKey));
    try {
      JsonNode itemValue =
          JsonToolkit.stringToJsonNode(
              Objects.requireNonNullElse(
                      retryTransactGetItems(
                              TransactGetItemsRequest.builder()
                                  .transactItems(
                                      TransactGetItem.builder()
                                          .get(Get.builder().tableName(tableName).key(key).build())
                                          .build())
                                  .build())
                          .responses()
                          .getFirst()
                          .item()
                          .get("value"),
                      AttributeValue.fromS("{}"))
                  .s());
      log.info(
          "RETURN _loadLockedItem(LB=%s, PK=%s, SK=%s, ...)"
              .formatted(lockedBy, partitionKey, sortKey));
      return itemValue;
    } catch (TransactionCanceledException transactionCanceledException) {
      log.info(
          "TransactionCanceled _loadLockedItem(LB=%s, PK=%s, SK=%s, ...)"
              .formatted(lockedBy, partitionKey, sortKey));
      log.warn(
          "Failed to get locked item value (exception='%s' reasons='%s')"
              .formatted(
                  transactionCanceledException,
                  transactionCanceledException.cancellationReasons().stream()
                      .map(reason -> "%s %s".formatted(reason.code(), reason.message()))
                      .collect(Collectors.joining(", "))));
      throw transactionCanceledException;
    }
  }

  @Override
  protected void _unlockItem(String lockedBy, String partitionKey, String sortKey) {
    log.info(
        "START _unlockItem(LB=%s, PK=%s, SK=%s, ...)".formatted(lockedBy, partitionKey, sortKey));
    String oldLockedUntil = Instant.now().toString();
    String newLockedUntil = Instant.ofEpochMilli(Instant.now().toEpochMilli() - 1).toString();
    Map<String, AttributeValue> key =
        Map.ofEntries(
            Map.entry("PK", AttributeValue.fromS(partitionKey)),
            Map.entry("SK", AttributeValue.fromS(sortKey)));
    try {
      _retryTransactWriteItems(
          TransactWriteItemsRequest.builder()
              .transactItems(
                  TransactWriteItem.builder()
                      .update(
                          Update.builder()
                              .tableName(tableName)
                              .key(key)
                              .conditionExpression(
                                  "attribute_exists(lockedBy) "
                                      + "AND lockedBy = :lb "
                                      + "AND lockedUntil > :olu")
                              .updateExpression("SET #lu = :nlu")
                              .expressionAttributeNames(
                                  Map.ofEntries(Map.entry("#lu", "lockedUntil")))
                              .expressionAttributeValues(
                                  Map.ofEntries(
                                      Map.entry(":lb", AttributeValue.fromS(lockedBy)),
                                      Map.entry(":olu", AttributeValue.fromS(oldLockedUntil)),
                                      Map.entry(":nlu", AttributeValue.fromS(newLockedUntil))))
                              .build())
                      .build())
              .build());
    } catch (TransactionCanceledException transactionCanceledException) {
      log.info(
          "TransactionCanceled _unlockItem(LB=%s, PK=%s, SK=%s, ...)".formatted(lockedBy, partitionKey, sortKey));
      log.warn(
          "Failed to unlock item (exception='%s' reasons='%s')"
              .formatted(
                  transactionCanceledException,
                  transactionCanceledException.cancellationReasons().stream()
                      .map(reason -> "%s %s".formatted(reason.code(), reason.message()))
                      .collect(Collectors.joining(", "))));
      throw transactionCanceledException;
    }
    log.info(
        "END _unlockItem(LB=%s, PK=%s, SK=%s, ...)".formatted(lockedBy, partitionKey, sortKey));
  }

  private TransactGetItemsResponse retryTransactGetItems(
      TransactGetItemsRequest transactGetItemsRequest) {
    TransactionCanceledException latestTransactionCanceledException = null;
    for (int retriesLeft = MAX_TRANSACTION_RETRY_COUNT - 1; retriesLeft >= 0; --retriesLeft) {
      try {
        return dynamoDbClient.transactGetItems(transactGetItemsRequest);
      } catch (TransactionCanceledException transactionCanceledException) {
        if (transactionCanceledException.cancellationReasons().stream()
            .noneMatch(reason -> "TransactionConflict".equals(reason.code()))) {
          throw transactionCanceledException;
        } else {
          latestTransactionCanceledException = transactionCanceledException;
          log.info("TransactGetItemsRequest %d retries left".formatted(retriesLeft));
        }
      }
    }
    throw new RuntimeException(
        "TransactGetItemsRequest failed after %d retries".formatted(MAX_TRANSACTION_RETRY_COUNT),
        latestTransactionCanceledException);
  }

  private void _retryTransactWriteItems(TransactWriteItemsRequest transactWriteItemsRequest) {
    TransactionCanceledException latestTransactionCanceledException = null;
    for (int retriesLeft = MAX_TRANSACTION_RETRY_COUNT - 1; retriesLeft >= 0; --retriesLeft) {
      try {
        dynamoDbClient.transactWriteItems(transactWriteItemsRequest);
        return;
      } catch (TransactionCanceledException transactionCanceledException) {
        if (transactionCanceledException.cancellationReasons().stream()
            .noneMatch(reason -> "TransactionConflict".equals(reason.code()))) {
          throw transactionCanceledException;
        } else {
          latestTransactionCanceledException = transactionCanceledException;
          log.info("TransactWriteItemsRequest %d retries left".formatted(retriesLeft));
        }
      }
    }
    throw new RuntimeException(
        "TransactWriteItemsRequest failed after %d retries".formatted(MAX_TRANSACTION_RETRY_COUNT),
        latestTransactionCanceledException);
  }
}
