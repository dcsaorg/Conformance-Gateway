package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.state.SortedPartitionsNonLockingMap;

import java.time.Instant;
import java.util.stream.Stream;

public class TrafficRecorder {
  private final SortedPartitionsNonLockingMap nonLockingMap;
  private final String partitionKey;

  public TrafficRecorder(SortedPartitionsNonLockingMap nonLockingMap, String partitionKey) {
    this.nonLockingMap = nonLockingMap;
    this.partitionKey = partitionKey;
  }

  public Stream<ConformanceExchange> getTrafficStream() {
    return nonLockingMap.getPartitionValues(partitionKey).stream()
        .map(jsonNode -> ConformanceExchange.fromJson((ObjectNode) jsonNode));
  }

  public void recordExchange(ConformanceExchange conformanceExchange) {
    nonLockingMap.setItemValue(
        partitionKey, Instant.now().toString(), conformanceExchange.toJson());
  }
}
