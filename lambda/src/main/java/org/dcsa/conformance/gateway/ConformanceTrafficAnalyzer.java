package org.dcsa.conformance.gateway;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.report.ConformanceReport;
import org.dcsa.conformance.gateway.configuration.StandardConfiguration;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

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
