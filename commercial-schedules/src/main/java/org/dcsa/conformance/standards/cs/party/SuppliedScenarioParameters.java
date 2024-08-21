package org.dcsa.conformance.standards.cs.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class SuppliedScenarioParameters {

  private final Map<CsFilterParameter, String> map;

  private SuppliedScenarioParameters(Map<CsFilterParameter, String> map) {
    this.map = Collections.unmodifiableMap(map);
  }

  public static SuppliedScenarioParameters fromMap(Map<CsFilterParameter, String> map) {
    return new SuppliedScenarioParameters(map);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
        Arrays.stream(CsFilterParameter.values())
            .filter(csFilterParameter -> jsonNode.has(csFilterParameter.getQueryParamName()))
            .collect(
                Collectors.toUnmodifiableMap(
                    Function.identity(),
                    csFilterParameter ->
                        jsonNode.required(csFilterParameter.getQueryParamName()).asText())));
  }

  public ObjectNode toJson() {
    ObjectNode objectNode = new ObjectMapper().createObjectNode();
    map.forEach(
        (csFilterParameter, value) -> objectNode.put(csFilterParameter.getQueryParamName(), value));
    return objectNode;
  }
}
