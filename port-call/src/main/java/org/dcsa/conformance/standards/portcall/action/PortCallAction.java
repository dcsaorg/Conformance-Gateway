package org.dcsa.conformance.standards.portcall.action;

import java.util.Map;
import java.util.function.Supplier;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.portcall.party.SuppliedScenarioParameters;

public class PortCallAction extends ConformanceAction {

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;

  @Override
  public String getHumanReadablePrompt() {
    return "";
  }

  protected PortCallAction(
    String sourcePartyName, String targetPartyName, PortCallAction previousAction, String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = _getSspSupplier(previousAction);
  }

  private Supplier<SuppliedScenarioParameters> _getSspSupplier(ConformanceAction previousAction) {
    return previousAction instanceof SupplyScenarioParametersAction supplyScenarioParametersAction
        ? supplyScenarioParametersAction::getSuppliedScenarioParameters
        : previousAction == null
            ? () -> SuppliedScenarioParameters.fromMap(Map.ofEntries())
            : _getSspSupplier(previousAction.getPreviousAction());
  }

  @Override
  public void reset() {
    super.reset();
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);

  }

}
