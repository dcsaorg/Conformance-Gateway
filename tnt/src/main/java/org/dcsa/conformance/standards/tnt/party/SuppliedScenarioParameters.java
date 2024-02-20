package org.dcsa.conformance.standards.tnt.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    ObjectNode objectNode = new ObjectMapper().createObjectNode();
    map.forEach(
      (ovsFilterParameter, value) ->
        objectNode.put(ovsFilterParameter.getQueryParamName(), value));
    return objectNode;
  }

  public static SuppliedScenarioParameters fromMap(Map<TntFilterParameter, String> map) {
    return new SuppliedScenarioParameters(map);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
      Arrays.stream(TntFilterParameter.values())
        .filter(ovsFilterParameter -> jsonNode.has(ovsFilterParameter.getQueryParamName()))
        .collect(
          Collectors.toUnmodifiableMap(
            Function.identity(),
            ovsFilterParameter ->
              jsonNode.required(ovsFilterParameter.getQueryParamName()).asText())));
  }
}
