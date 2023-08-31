package org.dcsa.conformance.gateway.check;

import org.dcsa.conformance.gateway.scenarios.ScenarioListBuilder;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10ConformanceCheck;

public enum ConformanceCheckFactory {
  ; // no instances

  public static ConformanceCheck create(
      String standardName, String standardVersion, ScenarioListBuilder<?> scenarioListBuilder) {
    if ("EblSurrender".equals(standardName) && "1.0.0".equals(standardVersion)) {
      return new EblSurrenderV10ConformanceCheck(scenarioListBuilder);
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'".formatted(standardName, standardVersion));
  }
}
