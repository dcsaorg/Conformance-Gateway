package org.dcsa.conformance.core.check;

import org.dcsa.conformance.core.report.ConformanceStatus;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ConformanceScenario;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ScenarioCheck extends ConformanceCheck {
  private final ConformanceScenario scenario;

  public ScenarioCheck(ConformanceScenario scenario) {
    super(scenario.getTitle());
    this.scenario = scenario;
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return scenario.allActionsStream().map(ConformanceAction::createCheck).filter(Objects::nonNull);
  }

  @Override
  public Consumer<ConformanceStatus> computedStatusConsumer() {
    return scenario.computedStatusConsumer();
  }
}
