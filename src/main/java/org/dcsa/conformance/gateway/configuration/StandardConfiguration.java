package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@ToString
public class StandardConfiguration {
  @Getter @Setter private String name;

  @Getter @Setter private String version;
}
