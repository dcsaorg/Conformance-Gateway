package org.dcsa.conformance.end.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Getter
public class SuppliedScenarioParameters {

  private final Map<EndorsementChainFilterParameter, String> map;

  private SuppliedScenarioParameters(Map<EndorsementChainFilterParameter, String> map) {
    this.map = Collections.unmodifiableMap(map);
  }

  public static SuppliedScenarioParameters fromMap(Map<EndorsementChainFilterParameter, String> map) {
    return new SuppliedScenarioParameters(map);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
      Arrays.stream(EndorsementChainFilterParameter.values())
        .filter(
          endorsementChainFilterParameter ->
            jsonNode.has(endorsementChainFilterParameter.getParamName()))
        .collect(
          Collectors.toUnmodifiableMap(
            Function.identity(),
            endorsementChainFilterParameter ->
              jsonNode
                .required(endorsementChainFilterParameter.getParamName())
                .asText())));
  }

  public ObjectNode toJson() {
    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
    map.forEach(
      (endorsementChainFilterParameter, value) ->
        objectNode.put(endorsementChainFilterParameter.getParamName(), value));
    return objectNode;
  }
}
