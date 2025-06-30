package org.dcsa.conformance.sandbox.state;

// PK=environment#UUID  SK=sandbox#UUID        value={id: sandboxUUID, name: sandboxName}
//
// PK=sandbox#UUID      SK=config              value={...}
// PK=sandbox#UUID      SK=state               value={currentSessionId: UUID, ...}  lock=...
// PK=sandbox#UUID      SK=waiting             value=[{"who": "Orchestrator", "forWhom": "Shipper1",
// "toDoWhat": "perform action 'UC1'"}, ...]
//
// PK=sandbox#UUID      SK=session#UTC#UUID
//
// PK=session#UUID      SK=state#orchestrator  value={...}                          lock=...
// PK=session#UUID      SK=state#party#NAME    value={...}                          lock=...
// PK=session#UUID      SK=exchange#UTC#UUID   value={...}
//
// PK=environment#UUID      SK=report#digest#<sandboxUUID>#<reportUTC> value={...title...standard...}
// PK=environment#UUID      SK=report#content#<sandboxUUID>#<reportUTC> value={...}

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.core.state.SortedPartitionsLockingMap;
import org.dcsa.conformance.core.state.SortedPartitionsNonLockingMap;
import org.dcsa.conformance.core.state.StatefulExecutor;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

@Getter
public class ConformancePersistenceProvider {
  /*
   * From the outside, items are viewed as:
   *  PK=itemPK / SK=itemSK / value=largeItemValue
   *
   * Internally, items are transformed into:
   *  PK=itemPK / SK=itemSK / value="DCSA_CONFORMANCE_CHUNKED_VALUE#<CHUNK_UUID>"
   *  PK=itemPK / SK=chunk#itemSK#<CHUNK_UUID>#00000000 / value=largeItemValue.substring(0 * MAX_VALUE_LENGTH, 1 * MAX_VALUE_LENGTH)
   *  PK=itemPK / SK=chunk#itemSK#<CHUNK_UUID>#00000001 / value=largeItemValue.substring(1 * MAX_VALUE_LENGTH, 2 * MAX_VALUE_LENGTH)
   *  ...
   *
   * When chunked values are modified, a new chunk UUID is used; old chunks are neither overwritten nor cleaned up.
   */

  private static final int DEFAULT_MAX_VALUE_LENGTH = 64 * 1024;
  private static final String DCSA_CONFORMANCE_CHUNKED_VALUE = "DCSA_CONFORMANCE_CHUNKED_VALUE";
  private final int maxValueLength;
  private final SortedPartitionsNonLockingMap nonLockingMap;
  private final StatefulExecutor statefulExecutor;

  public ConformancePersistenceProvider(
      SortedPartitionsNonLockingMap internalNonLockingMap,
      SortedPartitionsLockingMap internalLockingMap) {
    this(internalNonLockingMap, internalLockingMap, DEFAULT_MAX_VALUE_LENGTH);
  }

  ConformancePersistenceProvider(
      SortedPartitionsNonLockingMap internalNonLockingMap,
      SortedPartitionsLockingMap internalLockingMap,
      int maxValueLength) {
    this.maxValueLength = maxValueLength;
    this.nonLockingMap =
        new SortedPartitionsNonLockingMap() {
          @Override
          public void setItemValue(String partitionKey, String sortKey, JsonNode value) {
            String stringValue = value.toString();
            if (stringValue.length() <= maxValueLength) {
              internalNonLockingMap.setItemValue(partitionKey, sortKey, value);
              return;
            }
            String chunkUuid = UUID.randomUUID().toString();
            valueToChunks(getChunkSortKeyPrefix(sortKey, chunkUuid), stringValue)
                .forEach(
                    (chunkSortKey, chunkValue) ->
                        internalNonLockingMap.setItemValue(partitionKey, chunkSortKey, chunkValue));
            internalNonLockingMap.setItemValue(
                partitionKey, sortKey, getChunkValueRedirect(chunkUuid));
          }

          @Override
          public JsonNode getItemValue(String partitionKey, String sortKey) {
            JsonNode internalItemValue = internalNonLockingMap.getItemValue(partitionKey, sortKey);
            if (isNotChunkedValueRedirect(internalItemValue)) {
              return internalItemValue;
            }
            return chunksToValue(
                internalNonLockingMap.getPartitionValuesBySortKey(
                    partitionKey,
                    getChunkSortKeyPrefix(sortKey, getChunksUuid(internalItemValue))));
          }

          @Override
          public LinkedHashMap<String, JsonNode> getPartitionValuesBySortKey(
              String partitionKey, String sortKeyPrefix) {
            return internalNonLockingMap
                .getPartitionValuesBySortKey(partitionKey, sortKeyPrefix)
                .entrySet()
                .stream()
                .map(
                    entry -> {
                      if (isNotChunkedValueRedirect(entry.getValue())) {
                        return entry;
                      }
                      return Map.entry(
                          entry.getKey(),
                          chunksToValue(
                              internalNonLockingMap.getPartitionValuesBySortKey(
                                  partitionKey,
                                  getChunkSortKeyPrefix(
                                      entry.getKey(), getChunksUuid(entry.getValue())))));
                    })
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, updated) -> existing,
                        LinkedHashMap::new));
          }

          @Override
          public TreeMap<String, TreeMap<String, JsonNode>> scan(
              String partitionKeyPrefix, String sortKeyPrefix) {
            TreeMap<String, TreeMap<String, JsonNode>> externalResult = new TreeMap<>();
            internalNonLockingMap
                .scan(partitionKeyPrefix, sortKeyPrefix)
                .forEach(
                    (partitionKey, valuesBySortKey) ->
                        valuesBySortKey.forEach(
                            (sortKey, internalValue) ->
                                externalResult
                                    .computeIfAbsent(partitionKey, ignoredPK -> new TreeMap<>())
                                    .put(
                                        sortKey,
                                        isNotChunkedValueRedirect(internalValue)
                                            ? internalValue
                                            : chunksToValue(
                                                internalNonLockingMap.getPartitionValuesBySortKey(
                                                    partitionKey,
                                                    getChunkSortKeyPrefix(
                                                        sortKey, getChunksUuid(internalValue)))))));
            return externalResult;
          }
        };

    this.statefulExecutor =
        new StatefulExecutor(
            new SortedPartitionsLockingMap() {
              @Override
              public void saveItem(
                  String lockedBy, String partitionKey, String sortKey, JsonNode value) {
                String stringValue = value.toString();
                if (stringValue.length() <= maxValueLength) {
                  internalLockingMap.saveItem(lockedBy, partitionKey, sortKey, value);
                  return;
                }
                String chunkUuid = UUID.randomUUID().toString();
                valueToChunks(getChunkSortKeyPrefix(sortKey, chunkUuid), stringValue)
                    .forEach(
                        (chunkSorKey, chunkValue) ->
                            internalNonLockingMap.setItemValue(
                                partitionKey, chunkSorKey, chunkValue));
                internalLockingMap.saveItem(
                    lockedBy, partitionKey, sortKey, getChunkValueRedirect(chunkUuid));
              }

              @Override
              public JsonNode loadItem(String lockedBy, String partitionKey, String sortKey) {
                JsonNode internalItemValue =
                    internalLockingMap.loadItem(lockedBy, partitionKey, sortKey);
                if (isNotChunkedValueRedirect(internalItemValue)) {
                  return internalItemValue;
                }
                return chunksToValue(
                    internalNonLockingMap.getPartitionValuesBySortKey(
                        partitionKey,
                        getChunkSortKeyPrefix(sortKey, getChunksUuid(internalItemValue))));
              }

              @Override
              public void unlockItem(String lockedBy, String partitionKey, String sortKey) {
                internalLockingMap.unlockItem(lockedBy, partitionKey, sortKey);
              }
            });
  }

  private LinkedHashMap<String, JsonNode> valueToChunks(String chunkSortKeyPrefix, String value) {
    LinkedHashMap<String, JsonNode> chunksBySortKey = new LinkedHashMap<>();
    for (int chunkIndex = 0; chunkIndex * maxValueLength < value.length(); ++chunkIndex) {
      int chuckStart = chunkIndex * maxValueLength;
      int chunkEnd = Math.min(chuckStart + maxValueLength, value.length());
      String chunk = value.substring(chuckStart, chunkEnd);
      chunksBySortKey.put(
          "%s%08d".formatted(chunkSortKeyPrefix, chunkIndex), OBJECT_MAPPER.valueToTree(chunk));
    }
    return chunksBySortKey;
  }

  private JsonNode chunksToValue(LinkedHashMap<String, JsonNode> chunksBySortKey) {
    return JsonToolkit.stringToJsonNode(
        chunksBySortKey.values().stream().map(JsonNode::asText).collect(Collectors.joining()));
  }

  private boolean isNotChunkedValueRedirect(JsonNode internalItemValue) {
    return internalItemValue == null
        || !internalItemValue.isTextual()
        || !internalItemValue.asText().startsWith(DCSA_CONFORMANCE_CHUNKED_VALUE);
  }

  private JsonNode getChunkValueRedirect(String chunkUuid) {
    return OBJECT_MAPPER.valueToTree("%s#%s".formatted(DCSA_CONFORMANCE_CHUNKED_VALUE, chunkUuid));
  }

  private String getChunksUuid(JsonNode chunkValueRedirect) {
    return chunkValueRedirect.asText().split("#")[1];
  }

  private String getChunkSortKeyPrefix(String sortKey, String chunkUuid) {
    return "chunk#%s#%s#".formatted(sortKey, chunkUuid);
  }
}
