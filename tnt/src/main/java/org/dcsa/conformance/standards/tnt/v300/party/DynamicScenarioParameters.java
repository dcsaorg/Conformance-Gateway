package org.dcsa.conformance.standards.tnt.v300.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;
import org.dcsa.conformance.standards.tnt.v300.action.TntEventType;
import org.dcsa.conformance.standards.tnt.v300.checks.TntQueryParameters;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DynamicScenarioParameters(
    List<TntEventType> eventTypes, String cursor, String firstPage, String secondPage)
    implements ScenarioParameters {

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, DynamicScenarioParameters.class);
  }
}
