package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Supplier;

import lombok.Getter;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;

@Getter
public abstract class TdrAction extends ConformanceAction {
  protected final Supplier<String> tdrSupplier;
  private final int expectedStatus;

  public TdrAction(
      String sourcePartyName,
      String targetPartyName,
      int expectedStatus,
      ConformanceAction previousAction,
      String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.tdrSupplier = _getTdrSupplier(previousAction);
    this.expectedStatus = expectedStatus;
  }

  public abstract Supplier<String> getSrrSupplier();

  private Supplier<String> _getTdrSupplier(ConformanceAction previousAction) {
    return previousAction instanceof SupplyAvailableTdrAction supplyAvailableTdrAction
        ? supplyAvailableTdrAction.getTdrSupplier()
        : _getTdrSupplier(previousAction.getPreviousAction());
  }

  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("tdr", tdrSupplier.get());
  }
}
