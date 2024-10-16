package org.dcsa.conformance.standards.adoption.party;

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

  private final Map<FilterParameter, String> map;

  private SuppliedScenarioParameters(Map<FilterParameter, String> map) {
    this.map = Collections.unmodifiableMap(map);
  }

  public static SuppliedScenarioParameters fromMap(Map<FilterParameter, String> map) {
    return new SuppliedScenarioParameters(map);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
        Arrays.stream(FilterParameter.values())
            .filter(filterParameter -> jsonNode.has(filterParameter.getQueryParamName()))
            .collect(
                Collectors.toUnmodifiableMap(
                    Function.identity(),
                    filterParameter ->
                        jsonNode.required(filterParameter.getQueryParamName()).asText())));
  }

  public ObjectNode toJson() {
    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
    map.forEach(
        (csFilterParameter, value) -> objectNode.put(csFilterParameter.getQueryParamName(), value));
    return objectNode;
  }
}
