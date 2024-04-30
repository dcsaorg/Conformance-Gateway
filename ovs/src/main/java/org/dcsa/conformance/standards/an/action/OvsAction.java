package org.dcsa.conformance.standards.an.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.an.party.SuppliedScenarioParameters;

import java.util.Map;
import java.util.function.Supplier;

public abstract class OvsAction extends ConformanceAction {
  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  protected final int expectedStatus;

  public OvsAction(
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
    return previousAction instanceof SupplyScenarioParametersAction supplyAvailableTdrAction
        ? supplyAvailableTdrAction::getSuppliedScenarioParameters
        : previousAction == null
            ? () -> SuppliedScenarioParameters.fromMap(Map.ofEntries())
            : _getSspSupplier(previousAction.getPreviousAction());
  }
}
