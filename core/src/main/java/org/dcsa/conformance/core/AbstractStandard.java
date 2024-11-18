package org.dcsa.conformance.core;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import lombok.Getter;

@Getter
public abstract class AbstractStandard {
  private final String name;

  protected AbstractStandard(String name) {
    this.name = name;
  }

  public abstract SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion();

  public abstract Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName();

  protected abstract AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite);

  public AbstractComponentFactory createComponentFactory(
      String standardVersion, String scenarioSuite) {
    if (getScenarioSuitesByStandardVersion().keySet().stream()
        .noneMatch(version -> version.equals(standardVersion))) {
      throw new IllegalArgumentException(
          "Unsupported standard version '%s'".formatted(standardVersion));
    }
    if (getScenarioSuitesByStandardVersion().get(standardVersion).stream()
        .noneMatch(version -> version.equals(scenarioSuite))) {
      throw new IllegalArgumentException(
          "Unsupported scenario suite '%s'".formatted(scenarioSuite));
    }
    return doCreateComponentFactory(standardVersion, scenarioSuite);
  }
}
