package org.dcsa.conformance.standards.ebl.sb;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ConformanceScenario;

import java.util.*;
import java.util.function.Function;

@RequiredArgsConstructor(staticName = "newInstance")
public class ScenarioManager implements AutoCloseable {
  private String currentScenarioModuleName;
  private long moduleIndex = 0;
  private final List<ConformanceScenario> pendingScenarios = new ArrayList<>();
  private final Map<String, List<ConformanceScenario>> scenariosByModuleName;

  @Override
  public void close() {
    finishCurrentIfOpen();
  }

  public synchronized void openScenarioModule() {
    openScenarioModule("");
  }

  private void finishCurrentIfOpen() {
    if (this.currentScenarioModuleName != null) {
      this.finishModule();
    }
  }

  public synchronized void openScenarioModule(@NonNull String moduleName) {
    if (this.scenariosByModuleName.containsKey(moduleName) || Objects.equals(this.currentScenarioModuleName, moduleName)) {
      throw new IllegalArgumentException("The module name '%s' is already used".formatted(moduleName));
    }
    finishCurrentIfOpen();
    this.currentScenarioModuleName = moduleName;
  }

  public synchronized void finishModule() {
    if (currentScenarioModuleName == null) {
      throw new IllegalStateException("Cannot finish scenario before call to openScenarioModule()");
    }
    this.scenariosByModuleName.put(currentScenarioModuleName, List.copyOf(this.pendingScenarios));
    this.currentScenarioModuleName = null;
    this.pendingScenarios.clear();
    ++this.moduleIndex;
  }

  public synchronized <A extends ConformanceAction> void addScenario(ScenarioState<A, ?> state) {
    var currentState = state;
    List<Function<A, A>> actionGenerators = new LinkedList<>();

    if (currentScenarioModuleName == null) {
      throw new IllegalStateException("Cannot add a scenario between modules, call nextModule() first");
    }
    while (currentState != null) {
      var actionGenerator = currentState.getConformanceActionGenerator();
      if (actionGenerator != null) {
        actionGenerators.addFirst(actionGenerator);
      }
      currentState = currentState.getPreviousStepState();
    }
    if (actionGenerators.isEmpty()) {
      throw new IllegalArgumentException("There must be at least one action in the scenario");
    }
    A currentAction = null;
    List<ConformanceAction> actions = new ArrayList<>(actionGenerators.size());
    for (var generator : actionGenerators) {
      currentAction = generator.apply(currentAction);
      assert currentAction != null;
      actions.add(currentAction);
    }
    synchronized (this) {
      var idx = this.pendingScenarios.size();
      pendingScenarios.add(new ConformanceScenario(
        this.moduleIndex,
        idx,
        actions
      ));
    }
  }
}
