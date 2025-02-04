package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.*;
import org.dcsa.conformance.core.state.SortedPartitionsNonLockingMap;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public class TrafficRecorder {
  private final SortedPartitionsNonLockingMap nonLockingMap;
  private final String partitionKey;

  public TrafficRecorder(SortedPartitionsNonLockingMap nonLockingMap, String partitionKey) {
    this.nonLockingMap = nonLockingMap;
    this.partitionKey = partitionKey;
  }

  public synchronized Map<String, List<ConformanceExchange>> getTrafficByScenarioRun() {
    HashMap<String, List<ConformanceExchange>> trafficMap = new HashMap<>();
    nonLockingMap.getPartitionValuesBySortKey(partitionKey, "20").values().stream()
        .filter(itemNode -> itemNode.has("scenarioRun"))
        .forEach(
            itemNode ->
                trafficMap
                    .computeIfAbsent(
                        itemNode.get("scenarioRun").asText(), ignoredKey -> new ArrayList<>())
                    .add(ConformanceExchange.fromJson((ObjectNode) itemNode.get("exchange"))));
    return trafficMap;
  }

  public synchronized void recordExchange(ConformanceExchange conformanceExchange, String scenarioRun) {
    String sortKey;
    while (nonLockingMap.getItemValue(partitionKey, sortKey = Instant.now().toString()) != null) {
      try {
        this.wait(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    nonLockingMap.setItemValue(
        partitionKey,
        sortKey,
        OBJECT_MAPPER
            .createObjectNode()
            .put("scenarioRun", scenarioRun)
            .set("exchange", conformanceExchange.toJson()));
  }
}
