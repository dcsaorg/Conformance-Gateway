package org.dcsa.conformance.sandbox.state;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.state.SortedPartitionsLockingMap;
import org.dcsa.conformance.core.state.SortedPartitionsLockingMapException;
import org.dcsa.conformance.core.state.SortedPartitionsLockingMapExceptionCode;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

@Slf4j
public class DynamoDbSortedPartitionsLockingMap extends SortedPartitionsLockingMap {

  private static class MemoryMapItem {
    String lockedBy;
    long lockedUntil;
    JsonNode value;
  }

  private final HashMap<String, TreeMap<String, MemoryMapItem>> memoryMap = new HashMap<>();

  private final DynamoDbClient dynamoDbClient;
  private final String tableName;
  public DynamoDbSortedPartitionsLockingMap(DynamoDbClient dynamoDbClient, String tableName) {
    super(5 * 1000, 100, 10 * 1000);
    this.dynamoDbClient = dynamoDbClient;
    this.tableName = tableName;
  }

  private MemoryMapItem _getOrCreateItem(String partitionKey, String sortKey) {
    return memoryMap
        .computeIfAbsent(partitionKey, (ignoredKey) -> new TreeMap<>())
        .computeIfAbsent(sortKey, (ignoredKey) -> new MemoryMapItem());
  }

  @Override
  protected void _saveItem(String lockedBy, String partitionKey, String sortKey, JsonNode value)
      throws SortedPartitionsLockingMapException {
    dynamoDbClient.transactWriteItems(
            TransactWriteItemsRequest.builder()
                    .transactItems(
                            TransactWriteItem.builder()
                                    .put(
                                            Put.builder()
                                                    .tableName(tableName)
                                                    .item(Map.ofEntries(
                                                            Map.entry("PK", AttributeValue.fromS(partitionKey)),
                                                            Map.entry("SK", AttributeValue.fromS(sortKey)),
                                                            Map.entry("value", AttributeValue.fromS(value.toString()))
                                                    ))
                                                    .build())
                                    .build())
                    .build());
    synchronized (memoryMap) {
      MemoryMapItem item = _getOrCreateItem(partitionKey, sortKey);
      if (Objects.equals(lockedBy, item.lockedBy)) {
        if (item.lockedUntil > System.currentTimeMillis()) {
          item.value = value;
          item.lockedBy = null;
        } else {
          log.debug("%s cannot save: lock has expired".formatted(lockedBy));
          throw new SortedPartitionsLockingMapException(
              SortedPartitionsLockingMapExceptionCode.LOCK_HAS_EXPIRED, null);
        }
      } else {
        log.debug("%s cannot save: item is locked by %s".formatted(lockedBy, item.lockedBy));
        throw new SortedPartitionsLockingMapException(
            SortedPartitionsLockingMapExceptionCode.ITEM_NOT_LOCKED, null);
      }
    }
  }

  @Override
  protected JsonNode _loadItem(String lockedBy, String partitionKey, String sortKey)
      throws SortedPartitionsLockingMapException {
    synchronized (memoryMap) {
      MemoryMapItem item = _getOrCreateItem(partitionKey, sortKey);
      long currentTime = System.currentTimeMillis();
      if (item.lockedBy != null && item.lockedUntil > currentTime) {
        log.debug("%s cannot load: must wait for %s to save".formatted(lockedBy, item.lockedBy));
        throw new SortedPartitionsLockingMapException(
            SortedPartitionsLockingMapExceptionCode.ITEM_IS_LOCKED, null);
      } else {
        item.lockedBy = lockedBy;
        item.lockedUntil = currentTime + lockDurationMillis;
      }
      return item.value;
    }
  }

  @Override
  protected void _unlockItem(String lockedBy, String partitionKey, String sortKey)
      throws SortedPartitionsLockingMapException {
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
        throw new SortedPartitionsLockingMapException(
            SortedPartitionsLockingMapExceptionCode.ITEM_IS_LOCKED, null);
      }
    }
  }
}
