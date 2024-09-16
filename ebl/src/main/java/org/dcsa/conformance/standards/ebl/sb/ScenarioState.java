package org.dcsa.conformance.standards.ebl.sb;

import java.util.function.Function;
import org.dcsa.conformance.core.scenario.ConformanceAction;

public interface ScenarioState<A extends ConformanceAction, S extends ScenarioState<A, S>> {
  ScenarioState<A, S> getPreviousStepState();
  Function<A, A> getConformanceActionGenerator();
  S finishScenario();
  boolean isFinished();

}
