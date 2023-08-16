package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@ToString
@Getter
@Setter
public class PartyConfiguration {
  private String name;

  private String role;
}
