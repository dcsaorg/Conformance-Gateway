package org.dcsa.conformance.standards.jit.action;

import java.util.Map;
import java.util.function.Supplier;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.jit.party.SuppliedScenarioParameters;

public abstract class JitAction extends ConformanceAction {
  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  protected final int expectedStatus;

  public JitAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = _getSspSupplier(previousAction);
    this.expectedStatus = expectedStatus;
  }

  private Supplier<SuppliedScenarioParameters> _getSspSupplier(ConformanceAction previousAction) {
    return previousAction instanceof SupplyScenarioParametersAction supplyScenarioParametersAction
        ? supplyScenarioParametersAction::getSuppliedScenarioParameters
        : previousAction == null
            ? () -> SuppliedScenarioParameters.fromMap(Map.ofEntries())
            : _getSspSupplier(previousAction.getPreviousAction());
  }
}
