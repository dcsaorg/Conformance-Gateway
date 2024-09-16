package org.dcsa.conformance.standards.ebl.sb;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.dcsa.conformance.core.scenario.ConformanceAction;

public interface ScenarioBranchingStateBuilder<A extends ConformanceAction, S extends ScenarioState<A, S>> {


  ScenarioBranchingStateBuilder<A, S> singleStepBranch(Function<S, S> step);
  ScenarioBranchingStateBuilder<A, S> singleStepScenarioEndingBranch(Function<S, S> step);
  ScenarioBranchingStateBuilder<A, S> branch(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> stepper);
  ScenarioBranchingStateBuilder<A, S> scenarioEndingBranch(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> stepper);
  default BranchBuilder<A, S> inlineBranchBuilder() {
    return new BranchBuilderImpl<>(this);
  }

  ScenarioStepHandler<A, S> build();

  static <A extends ConformanceAction, S extends ScenarioState<A, S>> ScenarioBranchingStateBuilder<A, S> fromState(S s) {
    return new ScenarioBranchingStateBuilderImpl<>(new ScenarioSingleStateStepHandler<>(s));
  }
}

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ScenarioBranchingStateBuilderImpl<A extends ConformanceAction, S extends ScenarioState<A, S>> implements ScenarioBranchingStateBuilder<A, S> {

  private final ScenarioStepHandler<A, S> branchRoot;
  private final List<Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>>> branches = new ArrayList<>();
  private boolean isSpent = false;

  public ScenarioBranchingStateBuilder<A, S> singleStepBranch(Function<S, S> step) {
    this.branches.add((stepHandler) -> stepHandler.thenStep(step));
    return this;
  }

  @Override
  public ScenarioBranchingStateBuilder<A, S> singleStepScenarioEndingBranch(Function<S, S> step) {
    this.branches.add((stepHandler) -> stepHandler.thenStep(step).thenStep(S::finishScenario));
    return this;
  }

  @Override
  public ScenarioBranchingStateBuilder<A, S> scenarioEndingBranch(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> stepper) {
    this.branches.add(stepper.andThen(ScenarioStepHandler::finishScenario));
    return this;
  }

  public ScenarioBranchingStateBuilder<A, S> branch(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> stepper) {
    this.branches.add(stepper);
    return this;
  }

  public ScenarioStepHandler<A, S> build() {
    spend();
    return this.branchRoot.branches(this.branches);
  }

  private void spend() {
    if (isSpent) {
      throw new IllegalArgumentException("Incorrect usage of the branch builder; build() can only be called once");
    }
    this.isSpent = true;
  }

}
