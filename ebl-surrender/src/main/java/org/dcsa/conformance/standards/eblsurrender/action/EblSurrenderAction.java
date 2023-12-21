package org.dcsa.conformance.standards.eblsurrender.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Supplier;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.eblsurrender.party.SuppliedScenarioParameters;

@Getter
public abstract class EblSurrenderAction extends ConformanceAction {
  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  private final int expectedStatus;

  public EblSurrenderAction(
      String sourcePartyName,
      String targetPartyName,
      int expectedStatus,
      ConformanceAction previousAction,
      String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = _getSspSupplier(previousAction);
    this.expectedStatus = expectedStatus;
  }

  public abstract Supplier<String> getSrrSupplier();

  private Supplier<SuppliedScenarioParameters> _getSspSupplier(ConformanceAction previousAction) {
    return previousAction instanceof SupplyScenarioParametersAction supplyAvailableTdrAction
        ? supplyAvailableTdrAction::getSuppliedScenarioParameters
        : _getSspSupplier(previousAction.getPreviousAction());
  }

  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
  }
}
