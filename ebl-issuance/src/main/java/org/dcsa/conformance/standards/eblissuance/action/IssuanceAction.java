package org.dcsa.conformance.standards.eblissuance.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;

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

  protected abstract Supplier<String> getTdrSupplier();
}
