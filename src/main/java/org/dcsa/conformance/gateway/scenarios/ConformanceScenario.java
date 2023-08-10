package org.dcsa.conformance.gateway.scenarios;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public class ConformanceScenario {
    protected final LinkedList<ConformanceAction> nextActions = new LinkedList<>();

    public ConformanceScenario(Collection<ConformanceAction> actions) {
        this.nextActions.addAll(actions);
    }

    public ConformanceAction nextAction() {
        return nextActions.peek();
    }
}
