package org.dcsa.conformance.standards.jit.party;

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

  private final Map<JitFilterParameter, String> map;

  private SuppliedScenarioParameters(Map<JitFilterParameter, String> map) {
    this.map = Collections.unmodifiableMap(map);
  }

  public ObjectNode toJson() {
    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
    map.forEach(
        (jitFilterParameter, value) ->
            objectNode.put(jitFilterParameter.getQueryParamName(), value));
    return objectNode;
  }

  public static SuppliedScenarioParameters fromMap(Map<JitFilterParameter, String> map) {
    return new SuppliedScenarioParameters(map);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
        Arrays.stream(JitFilterParameter.values())
            .filter(jitFilterParameter -> jsonNode.has(jitFilterParameter.getQueryParamName()))
            .collect(
                Collectors.toUnmodifiableMap(
                    Function.identity(),
                    jitFilterParameter ->
                        jsonNode.required(jitFilterParameter.getQueryParamName()).asText())));
  }
}
