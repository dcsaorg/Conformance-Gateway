package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties(prefix = "conformance")
@ConfigurationPropertiesScan
@ToString
@Getter
@Setter
public class ConformanceConfiguration {
  private String standardName;
  private String standardVersion;
  private LinkConfiguration[] links = {};
}
