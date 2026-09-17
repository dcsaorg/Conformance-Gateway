package org.dcsa.conformance.standards.ebl.action;

import java.util.Set;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standardscommons.action.BookingAndEblAction;

/** A Carrier state-changing use case whose EBL notification exchange is optional. */
public abstract class CarrierNotificationEblAction extends StateChangingSIAction {

  protected CarrierNotificationEblAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAndEblAction previousAction,
      String actionTitle,
      int expectedStatus,
      boolean isWithNotifications) {
    super(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        actionTitle,
        expectedStatus,
        isWithNotifications);
  }

  @Override
  public Set<String> completableWithoutTrafficForRoles() {
    return Set.of(EblRole.CARRIER.getConfigName());
  }
}

