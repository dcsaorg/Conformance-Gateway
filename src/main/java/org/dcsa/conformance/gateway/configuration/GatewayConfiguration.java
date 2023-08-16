package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties(prefix = "gateway")
@ConfigurationPropertiesScan
@ToString
@Getter
@Setter
public class GatewayConfiguration {
  private LinkConfiguration[] links = {};
}
