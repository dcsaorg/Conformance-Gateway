package org.dcsa.conformance.standardscommons.action;

import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.standardscommons.party.BookingDynamicScenarioParameters;
import org.dcsa.conformance.standardscommons.party.EblDynamicScenarioParameters;

@Getter
public abstract class BookingAndEblAction extends ConformanceAction {

  private final OverwritingReference<EblDynamicScenarioParameters> eblDspReference;
  private final OverwritingReference<BookingDynamicScenarioParameters> bookingDspReference;

  protected BookingAndEblAction(
      String sourcePartyName,
      String targetPartyName,
      BookingAndEblAction previousAction,
      String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.eblDspReference = getEblDspReference(previousAction);
    this.bookingDspReference = getBookingDspReference(previousAction);
  }

  private OverwritingReference<EblDynamicScenarioParameters> getEblDspReference(
      BookingAndEblAction previousAction) {
    if (previousAction == null) {
      return new OverwritingReference<>(
          null, new EblDynamicScenarioParameters(null, null, null, null, null, false, false));
    }
    return new OverwritingReference<>(previousAction.eblDspReference, null);
  }

  private OverwritingReference<BookingDynamicScenarioParameters> getBookingDspReference(
      BookingAndEblAction previousAction) {
    if (previousAction == null) {
      return new OverwritingReference<>(
          null, new BookingDynamicScenarioParameters(null, null, null));
    }
    return new OverwritingReference<>(previousAction.bookingDspReference, null);
  }
}
