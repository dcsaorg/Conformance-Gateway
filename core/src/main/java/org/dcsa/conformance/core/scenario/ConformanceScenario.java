package org.dcsa.conformance.core.scenario;


import java.util.Collection;
import java.util.LinkedList;

public class ConformanceScenario {
  protected final LinkedList<ConformanceAction> nextActions = new LinkedList<>();

  public ConformanceScenario(Collection<ConformanceAction> actions) {
    this.nextActions.addAll(actions);
  }

  public boolean hasNextAction() {
    return !nextActions.isEmpty();
  }

  public ConformanceAction peekNextAction() {
    return nextActions.peek();
  }

  public ConformanceAction popNextAction() {
    return nextActions.pop();
  }

  @Override
  public String toString() {
    return "%s[%s]"
        .formatted(
            getClass().getSimpleName(),
            nextActions.isEmpty() ? "" : nextActions.peekLast().getActionPath());
  }
}