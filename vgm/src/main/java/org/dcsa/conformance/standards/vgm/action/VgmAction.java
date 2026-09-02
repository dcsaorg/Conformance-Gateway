package org.dcsa.conformance.standards.vgm.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.standards.vgm.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.vgm.party.SuppliedScenarioParameters;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class VgmAction extends ConformanceAction {

  private static final String CURRENT_DSP = "currentDsp";

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  private final OverwritingReference<DynamicScenarioParameters> dsp;

  protected VgmAction(
    String sourcePartyName,
    String targetPartyName,
    VgmAction previousAction,
    String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = getSspSupplier(previousAction);
    this.dsp =
      previousAction == null
        ? new OverwritingReference<>(null, new DynamicScenarioParameters(null, null))
        : new OverwritingReference<>(previousAction.dsp, null);
  }

  private Supplier<SuppliedScenarioParameters> getSspSupplier(ConformanceAction previousAction) {
    return previousAction
      instanceof SupplyScenarioParametersAction supplyScenarioParametersActionAction
      ? supplyScenarioParametersActionAction::getSuppliedScenarioParameters
      : previousAction == null
      ? () -> SuppliedScenarioParameters.fromMap(Map.ofEntries())
      : getSspSupplier(previousAction.getPreviousAction());
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dsp::get;
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dsp::set;
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
