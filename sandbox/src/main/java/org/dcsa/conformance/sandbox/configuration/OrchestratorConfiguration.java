package org.dcsa.conformance.sandbox.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrchestratorConfiguration {
  private int maxParallelScenarios = Integer.MAX_VALUE;
}
