package org.dcsa.conformance.standards.ovs.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.standards.ovs.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.ovs.party.SuppliedScenarioParameters;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class OvsAction extends ConformanceAction {

  private static final String CURRENT_DSP = "currentDsp";

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  private final OverwritingReference<DynamicScenarioParameters> dsp;

  public OvsAction(
    String sourcePartyName,
    String targetPartyName,
    ConformanceAction previousAction,
    String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = _getSspSupplier(previousAction);
    this.dsp = previousAction instanceof OvsAction previousOvsAction
      ? new OverwritingReference<>(previousOvsAction.dsp, null)
      : new OverwritingReference<>(null, new DynamicScenarioParameters(null, null));
  }

  private Supplier<SuppliedScenarioParameters> _getSspSupplier(ConformanceAction previousAction) {
    return previousAction instanceof SupplyScenarioParametersAction supplyAvailableTdrAction
      ? supplyAvailableTdrAction::getSuppliedScenarioParameters
      : previousAction == null
      ? () -> SuppliedScenarioParameters.fromMap(Map.ofEntries())
      : _getSspSupplier(previousAction.getPreviousAction());
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dsp::get;
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dsp::set;
  }

  @Override
  public void reset() {
    super.reset();
    if (previousAction != null) {
      this.dsp.set(null);
    } else {
      this.dsp.set(new DynamicScenarioParameters(null, null));
    }
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (dsp.hasCurrentValue()) {
      jsonState.set(CURRENT_DSP, dsp.get().toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode dspNode = jsonState.get(CURRENT_DSP);
    if (dspNode != null) {
      dsp.set(DynamicScenarioParameters.fromJson(dspNode));
    }
  }
}
