package org.dcsa.conformance.gateway.analysis;

import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.gateway.check.ConformanceCheck;
import org.dcsa.conformance.gateway.check.ConformanceCheckFactory;
import org.dcsa.conformance.gateway.scenarios.ScenarioListBuilder;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

public class ConformanceTrafficAnalyzer {
  private final String standardName;
  private final String standardVersion;

  public ConformanceTrafficAnalyzer(String standardName, String standardVersion) {
    this.standardName = standardName;
    this.standardVersion = standardVersion;
  }

  public Map<String, ConformanceReport> analyze(
          ScenarioListBuilder scenarioListBuilder,
          Stream<ConformanceExchange> trafficStream, String... roleNames) {
    ConformanceCheck conformanceCheck =
        ConformanceCheckFactory.create(standardName, standardVersion, scenarioListBuilder);
    trafficStream.forEach(conformanceCheck::check);
    return ConformanceReport.createForRoles(conformanceCheck, roleNames);
  }
}
