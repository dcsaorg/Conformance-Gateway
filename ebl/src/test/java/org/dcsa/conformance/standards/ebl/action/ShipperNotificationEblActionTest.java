package org.dcsa.conformance.standards.ebl.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShipperNotificationEblActionTest {

  @Test
  void everyShipperActionWithCarrierFollowUpUsesTheOptionalNotificationBase() {
    List<Class<?>> shipperNotificationActions =
        List.of(
            UC1_Shipper_SubmitShippingInstructionsAction.class,
            UC3ShipperSubmitUpdatedShippingInstructionsAction.class,
            UC5_Shipper_CancelUpdateToShippingInstructionsAction.class,
            UC7_Shipper_ApproveDraftTransportDocumentAction.class,
            UC17_Shipper_SubmitTransportDocumentAmendmentAction.class,
            UC18_Shipper_CancelTransportDocumentAmendmentAction.class);

    shipperNotificationActions.forEach(
        actionClass ->
            assertTrue(
                ShipperNotificationEblAction.class.isAssignableFrom(actionClass),
                actionClass.getSimpleName()));

    var action =
        new UC7_Shipper_ApproveDraftTransportDocumentAction(
            "Carrier", "Shipper", null, null, null, false);
    assertEquals(Set.of("Carrier"), action.completableWithoutTrafficForRoles());
  }
}

