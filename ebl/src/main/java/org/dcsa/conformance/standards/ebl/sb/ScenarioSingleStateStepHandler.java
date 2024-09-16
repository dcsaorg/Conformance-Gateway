package org.dcsa.conformance.standards.ebl.sb;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.conformance.core.scenario.ConformanceAction;

import static org.dcsa.conformance.standards.ebl.sb.ScenarioMultiStateStepHandler.mergeHandlers;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class ScenarioSingleStateStepHandler<A extends ConformanceAction, S extends ScenarioState<A, S>> implements ScenarioStepHandler<A, S> {

  @Getter
  /* package private */ final S state;
  private boolean actionTaken = false;

  @Override
  public ScenarioStepHandler<A, S> branchingStep(Function<ScenarioBranchingStateBuilder<A, S>, ScenarioStepHandler<A, S>> branchingStep) {
    spend();
    return mergeHandlers(Stream.of(branchingStep.apply(ScenarioBranchingStateBuilder.fromState(this.state))));
  }

  @Override
  public <T> ScenarioStepHandler<A, S> branchForEach(List<T> values, BiFunction<S, T, S> branchStep) {
    var resultingState = new ArrayList<S>();
    spend();
    for (T value : values) {
      resultingState.add(branchStep.apply(state, value));
    }
    return ScenarioStepHandler.fromStates(resultingState, false);
  }

  @Override
  public ScenarioStepHandler<A, S> branches(List<Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>>> handlers) {
    spend();
    return mergeHandlers(handlers.stream().map(branchingHandler -> branchingHandler.apply(new ScenarioSingleStateStepHandler<>(this.state))));
  }

  @Override
  public ScenarioStepHandler<A, S> then(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> stepImplementor) {
    spend();
    // We explicit clone here because if we passed "this" then the st
    return stepImplementor.apply(new ScenarioSingleStateStepHandler<>(this.state));
  }

  public ScenarioSingleStateStepHandler<A, S> thenStep(Function<S, S> singleStepImplementor) {
    spend();
    return new ScenarioSingleStateStepHandler<>(singleStepImplementor.apply(this.state));
  }

  public ScenarioStepHandler<A, S> finishScenario() {
    spend();
    return FinishedScenarioStepHandler.instance();
  }

  private void spend() {
    if (actionTaken) {
      throw new IllegalArgumentException("Incorrect usage of the step handler; Only one action can be taken. Use the branch methods for branching");
    }
    this.actionTaken = true;
  }

}
