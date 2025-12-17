package org.dcsa.conformance.standards.portcall.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class SuppliedScenarioParameters {
  private final Map<PortCallFilterParameter, String> map;

  private SuppliedScenarioParameters(Map<PortCallFilterParameter, String> map) {
    this.map = Collections.unmodifiableMap(map);
  }

  public static SuppliedScenarioParameters fromMap(Map<PortCallFilterParameter, String> map) {
    return new SuppliedScenarioParameters(map);
  }

  public ObjectNode toJson() {
    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
    map.forEach(
        (portCallFilterParameter, value) ->
            objectNode.put(portCallFilterParameter.getQueryParamName(), value));
    return objectNode;
  }

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    if (jsonNode == null || !jsonNode.isObject()) {
      return new SuppliedScenarioParameters(Collections.emptyMap());
    }

    Set<String> validKeys =
        Arrays.stream(PortCallFilterParameter.values())
            .map(PortCallFilterParameter::getQueryParamName)
            .collect(Collectors.toSet());

    Iterator<String> fieldNames = jsonNode.fieldNames();
    List<String> invalidFields = new ArrayList<>();
    while (fieldNames.hasNext()) {
      String field = fieldNames.next();
      if (!validKeys.contains(field)) {
        invalidFields.add(field);
      }
    }

    if (!invalidFields.isEmpty()) {
      throw new IllegalArgumentException(
          "Invalid query parameter(s): " + String.join(", ", invalidFields));
    }
    Map<PortCallFilterParameter, String> result =
        Arrays.stream(PortCallFilterParameter.values())
            .filter(param -> jsonNode.has(param.getQueryParamName()))
            .collect(
                Collectors.toUnmodifiableMap(
                    param -> param, param -> jsonNode.get(param.getQueryParamName()).asText()));

    return new SuppliedScenarioParameters(result);
  }
}
