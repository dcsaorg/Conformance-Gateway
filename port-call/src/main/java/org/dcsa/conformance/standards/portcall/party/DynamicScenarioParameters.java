package org.dcsa.conformance.standards.portcall.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;
import org.dcsa.conformance.standards.portcall.model.JitServiceTypeSelector;
import org.dcsa.conformance.standards.portcall.model.JitTimestamp;
import org.dcsa.conformance.standards.portcall.model.PortCallServiceTypeCode;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DynamicScenarioParameters(
    JitTimestamp previousTimestamp,
    JitTimestamp currentTimestamp,
    PortCallServiceTypeCode portCallServiceTypeCode,
    String portCallID,
    String terminalCallID,
    String portCallServiceID,
    JitServiceTypeSelector selector,
    boolean isFYI)
    implements ScenarioParameters {

  public DynamicScenarioParameters() {
    this(null, null, null, null, null, null, null, false);
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, DynamicScenarioParameters.class);
  }
}
