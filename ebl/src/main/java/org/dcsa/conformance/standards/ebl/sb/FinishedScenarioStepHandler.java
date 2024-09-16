package org.dcsa.conformance.standards.ebl.sb;

import org.dcsa.conformance.core.scenario.ConformanceAction;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

final class FinishedScenarioStepHandler<A extends ConformanceAction, S extends ScenarioState<A, S>> implements ScenarioStepHandler<A, S> {

  private static final FinishedScenarioStepHandler<?, ?> INSTANCE = new FinishedScenarioStepHandler<>();

  @SuppressWarnings("unchecked")
  static <A extends ConformanceAction, S extends ScenarioState<A, S>> ScenarioStepHandler<A, S> instance() {
    return (ScenarioStepHandler<A, S>) INSTANCE;
  }

  @Override
  public ScenarioStepHandler<A, S> then(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> thenStep) {
    throw new IllegalStateException("Scenario was already finished");
  }

  @Override
  public ScenarioStepHandler<A, S> thenStep(Function<S, S> thenStep) {
    throw new IllegalStateException("Scenario was already finished");
  }

  @Override
  public ScenarioStepHandler<A, S> finishScenario() {
    throw new IllegalStateException("Scenario was already finished");
  }

  public void assertScenariosAreFinished() {
    // By definition all is ok
  }

  @Override
  public ScenarioStepHandler<A, S> branchingStep(Function<ScenarioBranchingStateBuilder<A, S>, ScenarioStepHandler<A, S>> branchingStep) {
    throw new IllegalStateException("Scenario was already finished");
  }

  @Override
  public <T> ScenarioStepHandler<A, S> branchForEach(List<T> values, BiFunction<S, T, S> branchStep) {
    throw new IllegalStateException("Scenario was already finished");
  }

  @Override
  public ScenarioStepHandler<A, S> branches(List<Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>>> handlers) {
    throw new IllegalStateException("Scenario was already finished");
  }

}
