package org.dcsa.conformance.standards.booking.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

public abstract class BookingAndEblAction extends ConformanceAction {

  public final OverwritingReference<DynamicScenarioParameters> dspReference;

  protected BookingAndEblAction(
      String sourcePartyName,
      String targetPartyName,
      BookingAndEblAction previousAction,
      String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.dspReference = getDspReference(previousAction);
  }

  private OverwritingReference<DynamicScenarioParameters> getDspReference(
      BookingAndEblAction previousAction) {
    return previousAction == null
        ? new OverwritingReference<>(
            null,
            new DynamicScenarioParameters(
                null,
                null,
                null,
                null,
                null,
                false,
                OBJECT_MAPPER.createObjectNode(),
                OBJECT_MAPPER.createObjectNode(),
                null,
                null,
                null,
                null,
                null))
        : new OverwritingReference<>(previousAction.dspReference, null);
  }
}
