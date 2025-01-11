package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;

public interface SortedPartitionsNonLockingMap {
  void setItemValue(String partitionKey, String sortKey, JsonNode value);
  JsonNode getItemValue(String partitionKey, String sortKey);
  LinkedHashMap<String, JsonNode> getPartitionValuesBySortKey(String partitionKey, String sortKeyPrefix);
}
