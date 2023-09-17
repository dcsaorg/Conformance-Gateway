package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface SortedPartitionsNonLockingMap {
  void setItemValue(String partitionKey, String sortKey, JsonNode value);
  JsonNode getItemValue(String partitionKey, String sortKey);
  JsonNode getFirstItemValue(String partitionKey);
  JsonNode getLastItemValue(String partitionKey);
  List<JsonNode> getPartitionValues(String partitionKey);
}
