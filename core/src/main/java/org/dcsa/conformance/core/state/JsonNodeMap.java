package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JsonNodeMap {
  private final SortedPartitionsNonLockingMap nonLockingMap;
  private final String partitionKey;
  private final String sortKeyPrefix;

  public void save(String key, JsonNode value) {
    nonLockingMap.setItemValue(partitionKey, sortKeyPrefix + key, value);
  }

  public JsonNode load(String key) {
    return nonLockingMap.getItemValue(partitionKey, sortKeyPrefix + key);
  }
}
