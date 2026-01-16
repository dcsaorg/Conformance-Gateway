package org.dcsa.conformance.standards.tnt.v220.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class SuppliedScenarioParameters {

  private final Map<TntFilterParameter, String> map;

  private SuppliedScenarioParameters(Map<TntFilterParameter, String> map) {
    this.map = Collections.unmodifiableMap(map);
  }

  public ObjectNode toJson() {
    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
    map.forEach(
      (tntFilterParameter, value) ->
        objectNode.put(tntFilterParameter.getQueryParamName(), value));
    return objectNode;
  }

  public static SuppliedScenarioParameters fromMap(Map<TntFilterParameter, String> map) {
    return new SuppliedScenarioParameters(map);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
      Arrays.stream(TntFilterParameter.values())
        .filter(tntFilterParameter -> jsonNode.has(tntFilterParameter.getQueryParamName()))
        .collect(
          Collectors.toUnmodifiableMap(
            Function.identity(),
            tntFilterParameter ->
              jsonNode.required(tntFilterParameter.getQueryParamName()).asText())));
  }
}
