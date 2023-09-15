package org.dcsa.conformance.core.traffic;

import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.state.StatefulEntity;

@Slf4j
public class ConformanceTrafficRecorder implements StatefulEntity {

  private final LinkedHashMap<UUID, ConformanceExchange> traffic = new LinkedHashMap<>();

  @Override
  public JsonNode exportJsonState() {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode jsonState = objectMapper.createObjectNode();
    ArrayNode trafficArrayNode = objectMapper.createArrayNode();
    traffic.forEach(
        (key, value) -> {
          ObjectNode entryNode = objectMapper.createObjectNode();
          entryNode.put("uuid", key.toString());
          entryNode.set("exchange", value.toJson());
          trafficArrayNode.add(entryNode);
        });
    jsonState.set("traffic", trafficArrayNode);
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    StreamSupport.stream(jsonState.get("traffic").spliterator(), false)
        .forEach(
            entryNode ->
                traffic.put(
                    UUID.fromString(entryNode.get("uuid").asText()),
                    ConformanceExchange.fromJson((ObjectNode) entryNode.get("exchange"))));
  }

  public Stream<ConformanceExchange> getTrafficStream() {
    return this.traffic.values().stream();
  }

  public void recordExchange(ConformanceExchange conformanceExchange) {
    this.traffic.put(conformanceExchange.getUuid(), conformanceExchange);
  }
}
