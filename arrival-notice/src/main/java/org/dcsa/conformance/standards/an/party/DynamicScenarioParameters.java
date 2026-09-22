package org.dcsa.conformance.standards.an.party;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;

import java.util.Map;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DynamicScenarioParameters(
  Map<String, String> suppliedQueryParameters,
  String scenarioType,
  String cursor,
  String firstPageHash,
  String secondPageHash)
  implements ScenarioParameters {

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, DynamicScenarioParameters.class);
  }
}
