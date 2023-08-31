package org.dcsa.conformance.gateway.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RouteConfiguration {
    private String gatewayRootPath;
    private String sourcePartyName;
    private String sourcePartyRole;
    private String targetPartyName;
    private String targetPartyRole;
    private String targetRootPath;
    private String targetBaseUrl;
}
