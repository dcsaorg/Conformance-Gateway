package org.dcsa.conformance.gateway.parties;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dcsa.conformance.gateway.configuration.ConformanceConfiguration;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10Carrier;
import org.springframework.boot.autoconfigure.web.ServerProperties;

public enum ConformancePartiesFactory {
  ; // no instances

  public static Map<String, ConformanceParty> create(
      ServerProperties serverProperties, ConformanceConfiguration conformanceConfiguration) {
    if ("EblSurrender".equals(conformanceConfiguration.getStandardName())
        && "1.0.0".equals(conformanceConfiguration.getStandardVersion())) {
      String gatewayBaseUrl = serverProperties.getAddress().toString();
      return Arrays.stream(conformanceConfiguration.getLinks())
          .map(
              link ->
                  new EblSurrenderV10Carrier(
                      link.getSourceParty().getName(),
                      true,
                      gatewayBaseUrl,
                      link.getGatewayBasePath()))
          .collect(Collectors.toMap(ConformanceParty::getName, Function.identity()));
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'"
            .formatted(
                conformanceConfiguration.getStandardName(), conformanceConfiguration.getStandardVersion()));
  }
}
