package org.dcsa.conformance.standards.ebl.sb;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.dcsa.conformance.core.scenario.ConformanceAction;

public interface ScenarioBranchingStateBuilder<A extends ConformanceAction, S extends ScenarioState<A, S>> {


  ScenarioBranchingStateBuilder<A, S> singleStepBranch(Function<S, S> step);
  ScenarioBranchingStateBuilder<A, S> singleStepScenarioEndingBranch(Function<S, S> step);
  ScenarioBranchingStateBuilder<A, S> branch(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> stepper);

  /**
   * Create a branch that performs no change
   *
   * <p>This is useful if the scenario can optionally split off in a subpath that can merge back into the original
   * state. A common case is with "out of order" messages that is expected not to change the state (outcome). Here
   * you might want one path without the "out of order" message (the happy path) and one for all the out of order
   * messages cases. In this case, the happy path could use the noActionBranch() while the others would use one
   * of the branching operators.</p>
   *
   * @return The branching builder (this)
   */
  ScenarioBranchingStateBuilder<A, S> emptyBranch();
  ScenarioBranchingStateBuilder<A, S> scenarioEndingBranch(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> stepper);
  default BranchBuilder<A, S> inlineBranchBuilder() {
    return conditionalBranchBuilder((s) -> true);
  }
  BranchBuilder<A, S> conditionalBranchBuilder(Predicate<S> test);

  ScenarioStepHandler<A, S> build();

  static <A extends ConformanceAction, S extends ScenarioState<A, S>> ScenarioBranchingStateBuilder<A, S> fromState(S s) {
    return new ScenarioBranchingStateBuilderImpl<>(new ScenarioSingleStateStepHandler<>(s));
  }
}

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ScenarioBranchingStateBuilderImpl<A extends ConformanceAction, S extends ScenarioState<A, S>> implements ScenarioBranchingStateBuilder<A, S> {

  private final ScenarioSingleStateStepHandler<A, S> branchRoot;
  private final List<Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>>> branches = new ArrayList<>();
  private boolean isSpent = false;
  private int buildersCreated = 0;
  private int buildersFinished = 0;
  private boolean isEmptyBranchUsed = false;

  public ScenarioBranchingStateBuilder<A, S> emptyBranch() {
    ensureNotSpent();
    if (isEmptyBranchUsed) {
      throw new IllegalStateException("The emptyBranch() can only be used once (there is no point in having multiple of them).");
    }
    isEmptyBranchUsed = true;
    return singleStepBranch(Function.identity());
  }

  public ScenarioBranchingStateBuilder<A, S> singleStepBranch(Function<S, S> step) {
    ensureNotSpent();
    this.branches.add((stepHandler) -> stepHandler.thenStep(step));
    return this;
  }

  @Override
  public ScenarioBranchingStateBuilder<A, S> singleStepScenarioEndingBranch(Function<S, S> step) {
    ensureNotSpent();
    this.branches.add((stepHandler) -> stepHandler.thenStep(step).thenStep(S::finishScenario));
    return this;
  }

  @Override
  public ScenarioBranchingStateBuilder<A, S> scenarioEndingBranch(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> stepper) {
    ensureNotSpent();
    this.branches.add(stepper.andThen(ScenarioStepHandler::finishScenario));
    return this;
  }

  public ScenarioBranchingStateBuilder<A, S> branch(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> stepper) {
    ensureNotSpent();
    this.branches.add(stepper);
    return this;
  }

  public ScenarioStepHandler<A, S> build() {
    spend();
    if (this.buildersCreated != this.buildersFinished) {
      throw new IllegalStateException("The inlineBranchBuilder() was called %d time(s), but only %d of them finished their branch".formatted(buildersCreated, buildersFinished));
    }
    return this.branchRoot.branches(this.branches);
  }

  /*package private*/ void builderFinished() {
    ++this.buildersFinished;
  }

  @Override
  public BranchBuilder<A, S> conditionalBranchBuilder(Predicate<S> test) {
    ++this.buildersCreated;
    return new BranchBuilderImpl<>(this, test.test(this.branchRoot.getState()));
  }

  private void spend() {
    ensureNotSpent();
    this.isSpent = true;
  }

  private void ensureNotSpent() {
    if (isSpent) {
      throw new IllegalArgumentException("Incorrect usage of the branch builder; build() can only be called once");
    }
  }
}
