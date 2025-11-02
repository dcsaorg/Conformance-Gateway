package org.dcsa.conformance.standards.portcall.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;

@Getter
public class SuppliedScenarioParameters {
  private final Map<String, String> map;

  private SuppliedScenarioParameters(Map<String, String> map) {
    this.map = Collections.unmodifiableMap(map);
  }

  public ObjectNode toJson() {
    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
    map.forEach(
      (queryParam, value) ->
        objectNode.put(queryParam, value));
    return objectNode;
  }
}
