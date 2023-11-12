package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.booking.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BookingAction extends ConformanceAction {
  protected final int expectedStatus;

  public BookingAction(
      String sourcePartyName,
      String targetPartyName,
      BookingAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
  }

  protected BookingAction getPreviousBookingAction() {
    return (BookingAction) previousAction;
  }

  protected Consumer<CarrierScenarioParameters> getCspConsumer() {
    return getPreviousBookingAction().getCspConsumer();
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return getPreviousBookingAction().getDspConsumer();
  }

  protected Supplier<CarrierScenarioParameters> getCspSupplier() {
    return getPreviousBookingAction().getCspSupplier();
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return getPreviousBookingAction().getDspSupplier();
  }
}
