package org.dcsa.conformance.standards.vgm.action;

import java.util.Map;
import java.util.function.Supplier;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.vgm.party.SuppliedScenarioParameters;

public abstract class VgmAction extends ConformanceAction {

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;

  protected VgmAction(
      String sourcePartyName,
      String targetPartyName,
      VgmAction previousAction,
      String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = getSspSupplier(previousAction);
  }

  private Supplier<SuppliedScenarioParameters> getSspSupplier(ConformanceAction previousAction) {
    return previousAction
            instanceof SupplyScenarioParametersAction supplyScenarioParametersActionAction
        ? supplyScenarioParametersActionAction::getSuppliedScenarioParameters
        : previousAction == null
            ? () -> SuppliedScenarioParameters.fromMap(Map.ofEntries())
            : getSspSupplier(previousAction.getPreviousAction());
  }
}
