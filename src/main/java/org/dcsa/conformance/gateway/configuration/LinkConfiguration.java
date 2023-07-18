package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties
@ConfigurationPropertiesScan
@ToString
public class LinkConfiguration {

  @Getter @Setter private PartyConfiguration sourceParty;

  @Getter @Setter private PartyConfiguration targetParty;

  @Getter @Setter private String gatewayBasePath;

  @Getter @Setter private String targetBasePath;

  @Getter @Setter private String targetRootUrl;

  @Getter @Setter private String gatewayApiKey;

  @Getter @Setter private String targetApiKey;
}
