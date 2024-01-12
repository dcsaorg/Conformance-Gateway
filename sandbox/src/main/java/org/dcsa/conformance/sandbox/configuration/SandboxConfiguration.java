package org.dcsa.conformance.sandbox.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Getter
@Setter
@ToString
public class SandboxConfiguration {
  private String id;
  private String name;
  private String authHeaderName = "";
  private String authHeaderValue = "";
  private StandardConfiguration standard;
  private OrchestratorConfiguration orchestrator;
  private PartyConfiguration[] parties;
  private CounterpartConfiguration[] counterparts;

  public JsonNode toJsonNode() {
    return OBJECT_MAPPER.valueToTree(this);
  }

  @SneakyThrows
  public static SandboxConfiguration fromJsonNode(JsonNode jsonNode) {
    return OBJECT_MAPPER.treeToValue(jsonNode, SandboxConfiguration.class);
  }
}
