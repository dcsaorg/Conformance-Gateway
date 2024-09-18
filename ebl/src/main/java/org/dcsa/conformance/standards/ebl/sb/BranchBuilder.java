package org.dcsa.conformance.standards.ebl.sb;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.dcsa.conformance.core.scenario.ConformanceAction;

public interface BranchBuilder<A extends ConformanceAction, S extends ScenarioState<A, S>> {
  BranchBuilder<A, S> then(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> step);
  BranchBuilder<A, S> thenStep(Function<S, S> step);
  ScenarioBranchingStateBuilder<A, S> finishBranch();
  ScenarioBranchingStateBuilder<A, S> endScenario();
}

@RequiredArgsConstructor
class BranchBuilderImpl<A extends ConformanceAction, S extends ScenarioState<A, S>> implements BranchBuilder<A, S> {
  private final ScenarioBranchingStateBuilderImpl<A, S> builder;
  private final boolean isActive;
  private Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> combinedStep = (sh) -> sh;
  private boolean isSpent = false;
  private boolean hasSteps = false;

  @Override
  public BranchBuilder<A, S> thenStep(Function<S, S> step) {
    this.combinedStep = this.combinedStep.andThen(stepHandler -> stepHandler.thenStep(step));
    hasSteps = true;
    return this;
  }

  public BranchBuilder<A, S> then(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> step) {
    this.combinedStep = this.combinedStep.andThen(stepHandler -> stepHandler.then(step));
    hasSteps = true;
    return this;
  }

  public ScenarioBranchingStateBuilder<A, S> finishBranch() {
    spend();
    if (this.isActive) {
      return this.builder.branch(this.combinedStep);
    }
    return this.builder;
  }

  public ScenarioBranchingStateBuilder<A, S> endScenario() {
    spend();
    if (this.isActive) {
      return this.builder.scenarioEndingBranch(this.combinedStep);
    }
    return this.builder;
  }

  private void spend() {
    if (isSpent) {
      throw new IllegalStateException("Already spent/built");
    }
    if (!hasSteps) {
      throw new IllegalStateException("No steps were added to the branch! (Either a bug, or the branch builder is redundant, or it can be replaced with a simple step like scenarioEndingBranch)");
    }
    builder.builderFinished();
    this.isSpent = true;
  }
}
