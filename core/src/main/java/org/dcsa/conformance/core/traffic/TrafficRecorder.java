package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.*;
import org.dcsa.conformance.core.state.SortedPartitionsNonLockingMap;

public class TrafficRecorder {
  private final SortedPartitionsNonLockingMap nonLockingMap;
  private final String partitionKey;

  public TrafficRecorder(SortedPartitionsNonLockingMap nonLockingMap, String partitionKey) {
    this.nonLockingMap = nonLockingMap;
    this.partitionKey = partitionKey;
  }

  public Map<String, List<ConformanceExchange>> getTrafficByScenarioRun() {
    HashMap<String, List<ConformanceExchange>> trafficMap = new HashMap<>();
    nonLockingMap
        .getPartitionValues(partitionKey)
        .stream().filter(itemNode -> itemNode.has("scenarioRun"))
        .forEach(
            itemNode ->
                trafficMap
                    .computeIfAbsent(
                        itemNode.get("scenarioRun").asText(), ignoredKey -> new ArrayList<>())
                    .add(ConformanceExchange.fromJson((ObjectNode) itemNode.get("exchange"))));
    return trafficMap;
  }

  public void recordExchange(ConformanceExchange conformanceExchange, String scenarioRun) {
    nonLockingMap.setItemValue(
        partitionKey,
        Instant.now().toString(),
        new ObjectMapper()
            .createObjectNode()
            .put("scenarioRun", scenarioRun)
            .set("exchange", conformanceExchange.toJson()));
  }
}
