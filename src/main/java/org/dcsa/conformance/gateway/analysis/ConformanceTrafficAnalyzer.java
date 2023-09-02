package org.dcsa.conformance.gateway.analysis;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.dcsa.conformance.gateway.check.ConformanceCheck;
import org.dcsa.conformance.gateway.check.ConformanceCheckFactory;
import org.dcsa.conformance.gateway.configuration.StandardConfiguration;
import org.dcsa.conformance.gateway.scenarios.ScenarioListBuilder;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

public class ConformanceTrafficAnalyzer {
  private final StandardConfiguration standardConfiguration;

  public ConformanceTrafficAnalyzer(StandardConfiguration standardConfiguration) {
    this.standardConfiguration = standardConfiguration;
  }

  public Map<String, ConformanceReport> analyze(
      ScenarioListBuilder<?> scenarioListBuilder,
      Stream<ConformanceExchange> trafficStream,
      Set<String> roleNames) {
    ConformanceCheck conformanceCheck =
        ConformanceCheckFactory.create(standardConfiguration, scenarioListBuilder);
    trafficStream.forEach(conformanceCheck::check);
    return ConformanceReport.createForRoles(conformanceCheck, roleNames);
  }
}
