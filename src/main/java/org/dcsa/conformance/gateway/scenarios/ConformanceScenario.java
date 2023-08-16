package org.dcsa.conformance.gateway.scenarios;

import lombok.ToString;

import java.util.Collection;
import java.util.LinkedList;

@ToString
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
}
