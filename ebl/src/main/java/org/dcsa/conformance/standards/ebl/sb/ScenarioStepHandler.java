package org.dcsa.conformance.standards.ebl.sb;

import org.dcsa.conformance.core.scenario.ConformanceAction;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public sealed interface ScenarioStepHandler<A extends ConformanceAction, S extends ScenarioState<A, S>> permits ScenarioSingleStateStepHandler, ScenarioMultiStateStepHandler, FinishedScenarioStepHandler {

  ScenarioStepHandler<A, S> then(Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>> thenStep);

  ScenarioStepHandler<A, S> thenStep(Function<S, S> thenStep);

  ScenarioStepHandler<A, S> finishScenario();

  default void assertScenariosAreFinished() {
    throw new IllegalStateException("Not all paths that lead to here were finished");
  }

  ScenarioStepHandler<A, S> branchingStep(Function<ScenarioBranchingStateBuilder<A, S>, ScenarioStepHandler<A, S>> branchingStep);

  <T> ScenarioStepHandler<A, S> branchForEach(List<T> values, BiFunction<S, T, S> branchStep);

  default <T> ScenarioStepHandler<A, S> branchForEach(T[] values, BiFunction<S, T, S> branchStep) {
    return branchForEach(Arrays.asList(values), branchStep);
  }

  default <CE extends Class<E>, E extends Enum<E>> ScenarioStepHandler<A, S> branchForEach(CE enumClass, BiFunction<S, E, S> branchStep) {
    return branchForEach(enumClass.getEnumConstants(), branchStep);
  }

  ScenarioStepHandler<A, S> branches(List<Function<ScenarioSingleStateStepHandler<A, S>, ScenarioStepHandler<A, S>>> handlers);

  static <A extends ConformanceAction, S extends ScenarioState<A, S>> ScenarioSingleStateStepHandler<A, S> fromInitialState(S initialState) {
    if (initialState.isFinished())  {
      throw new IllegalArgumentException("The state must not be finished");
    }
    return new ScenarioSingleStateStepHandler<>(initialState);
  }

  static <A extends ConformanceAction, S extends ScenarioState<A, S>> ScenarioStepHandler<A, S> fromStates(List<S> states, boolean filteredEmpty) {
    if (!filteredEmpty && !states.isEmpty()) {
      states = states.stream().filter(s -> !s.isFinished()).toList();
      filteredEmpty = true;
    }
    if (filteredEmpty && states.isEmpty()) {
      return FinishedScenarioStepHandler.instance();
    }
    if (states.isEmpty()) {
      throw new IllegalStateException("No paths out of the state were made available");
    }
    if (states.size() == 1) {
      return new ScenarioSingleStateStepHandler<>(states.getFirst());
    }
    return new ScenarioMultiStateStepHandler<>(states);
  }

}
