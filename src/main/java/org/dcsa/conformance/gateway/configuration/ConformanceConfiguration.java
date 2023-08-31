package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties(prefix = "conformance")
@ConfigurationPropertiesScan
@Getter
@Setter
@ToString
public class ConformanceConfiguration {
  private StandardConfiguration standard;
  private GatewayConfiguration gateway;
  private OrchestratorConfiguration orchestrator;
  private PartyConfiguration[] parties;
}
