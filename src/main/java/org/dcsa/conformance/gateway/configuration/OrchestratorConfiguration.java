package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrchestratorConfiguration {
  private int maxParallelScenarios = Integer.MAX_VALUE;
  private String carrierName;
  private String platformName;
}
