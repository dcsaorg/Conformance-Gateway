package org.dcsa.conformance.standards.ebl.action;

import java.util.Set;
import org.dcsa.conformance.standards.ebl.party.EblRole;

/** A Shipper-initiated use case whose Carrier follow-up notification exchange is optional. */
public abstract class ShipperNotificationEblAction extends StateChangingSIAction {

  protected ShipperNotificationEblAction(
      String shipperPartyName,
      String carrierPartyName,
      EblAction previousAction,
      String actionTitle,
      int expectedStatus,
      boolean isWithNotifications) {
    super(
        shipperPartyName,
        carrierPartyName,
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

