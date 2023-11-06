package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;

import java.util.function.Supplier;

public abstract class BookingAction extends ConformanceAction {
  protected final int expectedStatus;

  public BookingAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
  }

  protected abstract Supplier<String> getCbrrSupplier();
}
