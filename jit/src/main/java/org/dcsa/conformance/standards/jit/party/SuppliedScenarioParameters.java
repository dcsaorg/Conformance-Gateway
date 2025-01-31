package org.dcsa.conformance.standards.jit.party;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.party.ScenarioParameters;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuppliedScenarioParameters(
    String portCallID,
    String terminalCallID,
    String portCallServiceID,
    PortCallServiceType serviceType)
    implements ScenarioParameters {

  public static SuppliedScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, SuppliedScenarioParameters.class);
  }
}
