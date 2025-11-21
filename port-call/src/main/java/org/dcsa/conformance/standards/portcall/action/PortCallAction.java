package org.dcsa.conformance.standards.portcall.action;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.portcall.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.portcall.party.SuppliedScenarioParameters;

public class PortCallAction extends ConformanceAction {

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  private final OverwritingReference<DynamicScenarioParameters> dsp;

  @Override
  public String getHumanReadablePrompt() {
    return "";
  }

  protected PortCallAction(
    String sourcePartyName, String targetPartyName, PortCallAction previousAction, String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = _getSspSupplier(previousAction);
    this.dsp =
        previousAction == null
            ? new OverwritingReference<>(null, new DynamicScenarioParameters(null))
            : new OverwritingReference<>(previousAction.dsp, null);
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
    if (previousAction != null) {
      this.dsp.set(null);
    } else {
      this.dsp.set(new DynamicScenarioParameters(null));
    }
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);

  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dsp::get;
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dsp::set;
  }
}
