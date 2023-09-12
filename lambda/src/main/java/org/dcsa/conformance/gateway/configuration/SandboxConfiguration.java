package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;

@Getter
@Setter
@ToString
public class SandboxConfiguration {
  private StandardConfiguration standard;
  private OrchestratorConfiguration orchestrator;
  private PartyConfiguration[] parties;
  private CounterpartConfiguration[] counterparts;
}
