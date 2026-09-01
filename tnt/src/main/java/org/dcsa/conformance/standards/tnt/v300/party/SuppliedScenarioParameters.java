package org.dcsa.conformance.standards.tnt.v300.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.party.ScenarioParameters;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Getter
public class SuppliedScenarioParameters implements ScenarioParameters {

  private final Map<TntQueryParameters, String> map;

  private SuppliedScenarioParameters(Map<TntQueryParameters, String> map) {
    this.map = Collections.unmodifiableMap(map);
  }

  public static SuppliedScenarioParameters fromMap(Map<TntQueryParameters, String> map) {
    return new SuppliedScenarioParameters(map);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
      Arrays.stream(TntQueryParameters.values())
        .filter(tntQueryParameters -> jsonNode.has(tntQueryParameters.getParameterName()))
        .collect(
          Collectors.toUnmodifiableMap(
            Function.identity(),
            tntQueryParameters ->
              jsonNode.required(tntQueryParameters.getParameterName()).asText())));
  }

  @Override
  public ObjectNode toJson() {
    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
    map.forEach(
      (tntQueryParameters, value) ->
        objectNode.put(tntQueryParameters.getParameterName(), value));
    return objectNode;
  }
}
