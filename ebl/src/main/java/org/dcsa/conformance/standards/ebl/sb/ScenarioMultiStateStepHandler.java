package org.dcsa.conformance.standards.ebl.sb;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.dcsa.conformance.core.scenario.ConformanceAction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ScenarioMultiStateStepHandler<A extends ConformanceAction, S extends ScenarioState<A, S>> implements ScenarioStepHandler<A, S> {

  /* package private */ final List<S> states;
  private boolean actionTaken = false;

  @Override
  public ScenarioStepHandler<A, S> then(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> stepImplementor) {
    spend();
    return mergeHandlers(this.states.stream().map(ScenarioSingleStateStepHandler::new).map(stepImplementor));
  }

  @Override
  public ScenarioStepHandler<A, S> thenStep(Function<S, S> thenStep) {
    spend();
    return new ScenarioMultiStateStepHandler<>(this.states.stream().map(thenStep).toList());
  }

  @Override
  public ScenarioStepHandler<A, S> branchingStep(Function<ScenarioBranchingStateBuilder<A, S>, ScenarioStepHandler<A, S>> branchingStep) {
    spend();
    return mergeHandlers(this.states.stream().map(s -> branchingStep.apply(ScenarioBranchingStateBuilder.fromState(s))));
  }

  @Override
  public <T> ScenarioStepHandler<A, S> branchForEach(List<T> values, BiFunction<S, T, S> branchStep) {
    var resultingState = new ArrayList<S>();
    spend();
    for (T value : values) {
      this.states.stream().map(s -> branchStep.apply(s, value)).forEach(resultingState::add);
    }
    return new ScenarioMultiStateStepHandler<>(resultingState);
  }

  @Override
  public ScenarioStepHandler<A, S> branches(List<Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>>> handlers) {
    spend();

    return mergeHandlers(
        this.states.stream()
            .flatMap(
                s -> handlers.stream().map(b -> b.apply(new ScenarioSingleStateStepHandler<>(s)))));
  }

  public ScenarioStepHandler<A, S> finishScenario() {
    spend();
    states.forEach(S::finishScenario);
    return FinishedScenarioStepHandler.instance();
  }

  @Override
  public void assertScenariosAreFinished() {
    var actions = ScenarioManager.generateAction(states.getFirst());
    var path = actions.getLast().getActionPath();
    throw new IllegalStateException("At %d scenarios were not finished. Example being: %s".formatted(states.size(), path));
  }

  private void spend() {
    if (actionTaken) {
      throw new IllegalArgumentException("Incorrect usage of the step handler; Only one action can be taken. Use the branch methods for branching");
    }
    this.actionTaken = true;
  }

  static <A extends ConformanceAction, S extends ScenarioState<A, S>> ScenarioStepHandler<A, S> mergeHandlers(Stream<ScenarioStepHandler<A, S>> handlers) {
    AtomicBoolean sawFinishedChainer = new AtomicBoolean(false);
    var resultingStates = handlers.flatMap(chainer ->
        switch (chainer) {
          case ScenarioMultiStateStepHandler<A, S> ms -> ms.states.stream();
          case ScenarioSingleStateStepHandler<A, S> ss -> Stream.of(ss.state);
          case FinishedScenarioStepHandler<A, S> ignored -> {
            sawFinishedChainer.set(true);
            yield Stream.empty();
          }
        })
      .toList();
    if (resultingStates.isEmpty() && sawFinishedChainer.get()) {
      return FinishedScenarioStepHandler.instance();
    }
    // The other scenario step handlers could include finished states, so we set filteredEmpty to false
    // such that fromStates will do the filtering for us.
    return ScenarioStepHandler.fromStates(resultingStates, false);
  }

}
