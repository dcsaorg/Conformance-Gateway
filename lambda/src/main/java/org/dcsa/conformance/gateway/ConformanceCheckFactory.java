package org.dcsa.conformance.gateway;

import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.gateway.configuration.StandardConfiguration;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10ConformanceCheck;

public enum ConformanceCheckFactory {
  ; // no instances

  public static ConformanceCheck create(
      StandardConfiguration standardConfiguration, ScenarioListBuilder<?> scenarioListBuilder) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {
      return new EblSurrenderV10ConformanceCheck(scenarioListBuilder);
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'"
            .formatted(standardConfiguration.getName(), standardConfiguration.getVersion()));
  }
}
