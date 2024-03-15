package org.dcsa.conformance.standards.eblissuance.action;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.standards.eblissuance.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.eblissuance.party.SuppliedScenarioParameters;

public abstract class IssuanceAction extends ConformanceAction {
  private final OverwritingReference<DynamicScenarioParameters> dspReference;
  protected final int expectedStatus;

  public IssuanceAction(
      String sourcePartyName,
      String targetPartyName,
      IssuanceAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
    if (previousAction == null) {
      this.dspReference =
          new OverwritingReference<>(null, new DynamicScenarioParameters(EblType.STRAIGHT_EBL));
    } else {
      this.dspReference = new OverwritingReference<>(previousAction.dspReference, null);
    }
  }

  protected IssuanceAction getPreviousIssuanceAction() {
    return (IssuanceAction) previousAction;
  }


  public DynamicScenarioParameters getDsp() {
    return this.dspReference.get();
  }

  public void setDsp(DynamicScenarioParameters dsp) {
    this.dspReference.set(dsp);
  }

  @Override
  public ObjectNode exportJsonState() {
    var state = super.exportJsonState();
    if (dspReference.hasCurrentValue()) {
      state.set("dsp", getDsp().toJson());
    }
    return state;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    if (jsonState.has("dsp")) {
      this.setDsp(DynamicScenarioParameters.fromJson(jsonState.path("dsp")));
    }
  }

  protected Consumer<SuppliedScenarioParameters> getSspConsumer() {
    return getPreviousIssuanceAction().getSspConsumer();
  }

  protected Supplier<SuppliedScenarioParameters> getSspSupplier() {
    return getPreviousIssuanceAction().getSspSupplier();
  }

  protected abstract Supplier<String> getTdrSupplier();
}
