package org.dcsa.conformance.standards.an.party;

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

  private final Map<ArrivalNoticeFilterParameter, String> map;

  private SuppliedScenarioParameters(Map<ArrivalNoticeFilterParameter, String> map) {
    this.map = Collections.unmodifiableMap(map);
  }

  public ObjectNode toJson() {
    ObjectNode objectNode = new ObjectMapper().createObjectNode();
    map.forEach(
        (anFilterParameter, value) ->
            objectNode.put(anFilterParameter.getQueryParamName(), value));
    return objectNode;
  }

  public static SuppliedScenarioParameters fromMap(Map<ArrivalNoticeFilterParameter, String> map) {
    return new SuppliedScenarioParameters(map);
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return new SuppliedScenarioParameters(
        Arrays.stream(ArrivalNoticeFilterParameter.values())
            .filter(anFilterParameter -> jsonNode.has(anFilterParameter.getQueryParamName()))
            .collect(
                Collectors.toUnmodifiableMap(
                    Function.identity(),
                    anFilterParameter ->
                        jsonNode.required(anFilterParameter.getQueryParamName()).asText())));
  }
}
