package org.dcsa.conformance.core.check;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.dcsa.conformance.core.report.ConformanceStatus;
import org.dcsa.conformance.core.scenario.ConformanceScenario;

public class ScenarioCheck extends ConformanceCheck {
  private final ConformanceScenario scenario;
  private final String expectedApiVersion;

  public ScenarioCheck(ConformanceScenario scenario, String expectedApiVersion) {
    super(scenario.getTitle());
    this.scenario = scenario;
    this.expectedApiVersion = expectedApiVersion;
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return scenario
        .allActionsStream()
        .map(action -> action.createFullCheck(expectedApiVersion))
        .filter(Objects::nonNull);
  }

  @Override
  public Consumer<ConformanceStatus> computedStatusConsumer() {
    return scenario.computedStatusConsumer();
  }
}
