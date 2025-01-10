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

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.core.state.*;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

@Getter
public class ConformancePersistenceProvider {
  private static final int MAX_VALUE_LENGTH = 8 * 1024; // FIXME after testing reset this to 128 * 1024
  public static final String DCSA_CONFORMANCE_CHUNKED_VALUE = "DCSA_CONFORMANCE_CHUNKED_VALUE";
  private final SortedPartitionsNonLockingMap nonLockingMap;
  private final StatefulExecutor statefulExecutor;

  private static LinkedHashMap<String, JsonNode> valueToChunks(String sortKey, String value) {
    LinkedHashMap<String, JsonNode> chunksBySortKey = new LinkedHashMap<>();
    for (int chunkIndex = 0;
         chunkIndex * MAX_VALUE_LENGTH < value.length();
         ++chunkIndex) {
      int chuckStart = chunkIndex * MAX_VALUE_LENGTH;
      int chunkEnd = Math.min(chuckStart + MAX_VALUE_LENGTH, value.length());
      String chunk = value.substring(chuckStart, chunkEnd);
      chunksBySortKey.put(
          "chunk#%s#%08d".formatted(sortKey, chunkIndex), OBJECT_MAPPER.valueToTree(chunk));
    }
    return chunksBySortKey;
  }

  private static JsonNode chunksToValue(LinkedHashMap<String, JsonNode> chunksBySortKey) {
    return JsonToolkit.stringToJsonNode(
        chunksBySortKey.values().stream().map(JsonNode::asText).collect(Collectors.joining()));
  }

  public ConformancePersistenceProvider(
      SortedPartitionsNonLockingMap internalNonLockingMap,
      SortedPartitionsLockingMap internalLockingMap) {
    /*
     * From the outside, items are viewed as:
     *  PK=itemPK / SK=itemSK / value=largeItemValue
     * Internally, items are transformed into:
     *  PK=itemPK / SK=itemSK / value="DCSA_CONFORMANCE_CHUNKED_VALUE"
     *  PK=itemPK / SK=chunk#itemSK#00000000 / value=largeItemValue.substring(0 * MAX_VALUE_LENGTH, 1 * MAX_VALUE_LENGTH)
     *  PK=itemPK / SK=chunk#itemSK#00000001 / value=largeItemValue.substring(1 * MAX_VALUE_LENGTH, 2 * MAX_VALUE_LENGTH)
     *  ...
     */
    this.nonLockingMap =
        new SortedPartitionsNonLockingMap() {
          @Override
          public void setItemValue(String partitionKey, String sortKey, JsonNode value) {
            String stringValue = value.toString();
            if (stringValue.length() <= MAX_VALUE_LENGTH) {
              internalNonLockingMap.setItemValue(partitionKey, sortKey, value);
              return;
            }
            valueToChunks(sortKey, stringValue)
                .forEach(
                    (chunkSorKey, chunkValue) ->
                        internalNonLockingMap.setItemValue(partitionKey, chunkSorKey, chunkValue));
            // FIXME append a random UUID to the chunk prefix to support updates
            internalNonLockingMap.setItemValue(
                partitionKey, sortKey, OBJECT_MAPPER.valueToTree(DCSA_CONFORMANCE_CHUNKED_VALUE));
          }

          @Override
          public JsonNode getItemValue(String partitionKey, String sortKey) {
            JsonNode internalItemValue = internalNonLockingMap.getItemValue(partitionKey, sortKey);
            if (internalItemValue == null
                || !internalItemValue.isTextual()
                || !DCSA_CONFORMANCE_CHUNKED_VALUE.equals(internalItemValue.asText())) {
              return internalItemValue;
            }
            return chunksToValue(
                internalNonLockingMap.getPartitionValuesBySortKey(
                    partitionKey, "chunk#%s#".formatted(sortKey)));
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
                      if (!entry.getValue().isTextual()
                          || !DCSA_CONFORMANCE_CHUNKED_VALUE.equals(entry.getValue().asText())) {
                        return entry;
                      }
                      return Map.entry(
                          entry.getKey(),
                          chunksToValue(
                              internalNonLockingMap.getPartitionValuesBySortKey(
                                  partitionKey, "chunk#%s#".formatted(entry.getKey()))));
                    })
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, updated) -> existing,
                        LinkedHashMap::new));
          }
        };

    this.statefulExecutor =
        new StatefulExecutor(
            new SortedPartitionsLockingMap() {
              @Override
              public JsonNode loadItem(String lockedBy, String partitionKey, String sortKey) {
                return null;
              }

              @Override
              public void saveItem(
                  String lockedBy, String partitionKey, String sortKey, JsonNode value) {}

              @Override
              public void unlockItem(String lockedBy, String partitionKey, String sortKey) {}
            });
  }
}
