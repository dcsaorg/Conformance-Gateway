package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@Getter
@Setter
@ToString
public class PartyConfiguration {
  private String name;
  private String role;
  private String counterpartBaseUrl;
  private String counterpartRootPath;
}
