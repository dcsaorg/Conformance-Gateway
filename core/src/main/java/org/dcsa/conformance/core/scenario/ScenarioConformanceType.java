package org.dcsa.conformance.core.scenario;

public enum ScenarioConformanceType {
  REQUIRED,
  OPTIONAL;

  public boolean affectsOverallConformance() {
    return this == REQUIRED;
  }
}

