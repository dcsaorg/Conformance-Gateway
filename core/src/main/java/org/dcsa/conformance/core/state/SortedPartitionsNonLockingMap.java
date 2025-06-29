package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.TreeMap;

public interface SortedPartitionsNonLockingMap {
  void setItemValue(String partitionKey, String sortKey, JsonNode value);

  JsonNode getItemValue(String partitionKey, String sortKey);

  LinkedHashMap<String, JsonNode> getPartitionValuesBySortKey(
      String partitionKey, String sortKeyPrefix);

  TreeMap<String, TreeMap<String, JsonNode>> scan(String partitionKeyPrefix, String sortKeyPrefix);
}
