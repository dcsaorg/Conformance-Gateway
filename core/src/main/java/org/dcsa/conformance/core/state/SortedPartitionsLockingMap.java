package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;

public interface SortedPartitionsLockingMap {
  JsonNode loadItem(String lockedBy, String partitionKey, String sortKey);

  void saveItem(String lockedBy, String partitionKey, String sortKey, JsonNode value);

  void unlockItem(String lockedBy, String partitionKey, String sortKey);
}
