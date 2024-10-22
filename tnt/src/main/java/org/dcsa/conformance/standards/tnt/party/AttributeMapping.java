package org.dcsa.conformance.standards.tnt.party;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.util.Set;
import java.util.function.BiPredicate;

@Getter
public class AttributeMapping {
  private final String jsonPath;
  private final BiPredicate<JsonNode, String> condition;
  private final Set<String> values; // Values to filter by (can be empty for no filtering)

  public AttributeMapping(String jsonPath, BiPredicate<JsonNode, String> condition, Set<String> values) {
    this.jsonPath = jsonPath;
    this.condition = condition;
    this.values = values;
  }
}
