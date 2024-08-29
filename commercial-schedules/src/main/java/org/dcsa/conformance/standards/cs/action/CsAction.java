package org.dcsa.conformance.standards.cs.action;

import java.util.Map;
import java.util.function.Supplier;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.cs.party.SuppliedScenarioParameters;

public abstract class CsAction extends ConformanceAction {

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  protected final int expectedStatus;

  public CsAction(
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
    return previousAction
            instanceof SupplyScenarioParametersAction supplyScenarioParametersActionAction
        ? supplyScenarioParametersActionAction::getSuppliedScenarioParameters
        : previousAction == null
            ? () -> SuppliedScenarioParameters.fromMap(Map.ofEntries())
            : _getSspSupplier(previousAction.getPreviousAction());
  }
}