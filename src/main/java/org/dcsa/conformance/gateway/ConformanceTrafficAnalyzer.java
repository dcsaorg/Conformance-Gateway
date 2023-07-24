package org.dcsa.conformance.gateway;

import org.dcsa.conformance.gateway.configuration.GatewayConfiguration;

import java.util.stream.Stream;

public class ConformanceTrafficAnalyzer {
  private final GatewayConfiguration gatewayConfiguration;
  private final String standardName;
  private final String standardVersion;
  private final String partyName;

  public ConformanceTrafficAnalyzer(
      GatewayConfiguration gatewayConfiguration,
      String standardName,
      String standardVersion,
      String partyName) {
    this.gatewayConfiguration = gatewayConfiguration;
    this.standardName = standardName;
    this.standardVersion = standardVersion;
    this.partyName = partyName;
  }

  public ConformanceReport analyze(
      String checkedPartyName, String checkedRoleName, Stream<ConformanceExchange> trafficStream) {
    ConformanceCheck conformanceCheck =
        ConformanceCheckFactory.create(
            gatewayConfiguration, checkedPartyName, checkedRoleName, standardName, standardVersion);
    trafficStream.forEach(exchange -> conformanceCheck.check(exchange));
    return new ConformanceReport(conformanceCheck);
  }
}
