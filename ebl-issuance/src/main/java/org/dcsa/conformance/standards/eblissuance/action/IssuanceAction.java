package org.dcsa.conformance.standards.eblissuance.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.eblissuance.party.SuppliedScenarioParameters;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class IssuanceAction extends ConformanceAction {
  protected final int expectedStatus;

  public IssuanceAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
  }

  protected IssuanceAction getPreviousIssuanceAction() {
    return (IssuanceAction) previousAction;
  }

  protected Consumer<SuppliedScenarioParameters> getSspConsumer() {
    return getPreviousIssuanceAction().getSspConsumer();
  }

  protected Supplier<SuppliedScenarioParameters> getSspSupplier() {
    return getPreviousIssuanceAction().getSspSupplier();
  }

  protected abstract Supplier<String> getTdrSupplier();
}
