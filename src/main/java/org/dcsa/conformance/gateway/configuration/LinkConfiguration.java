package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties
@ConfigurationPropertiesScan
@ToString
@Getter
@Setter
public class LinkConfiguration {

  private PartyConfiguration sourceParty;

  private PartyConfiguration targetParty;

  private String gatewayBasePath;

  private String targetBasePath;

  private String targetRootUrl;

  private String gatewayApiKey;

  private String targetApiKey;
}
