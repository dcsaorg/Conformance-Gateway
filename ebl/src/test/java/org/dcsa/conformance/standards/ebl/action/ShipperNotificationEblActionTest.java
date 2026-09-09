package org.dcsa.conformance.standards.ebl.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.dcsa.conformance.core.check.ConformanceCheck;
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
            UC15_Shipper_CancelShippingInstructionsAction.class,
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

    List<String> primaryCheckTitles =
        action
            .createCheck("3.0.0")
            .subChecksStream()
            .map(ConformanceCheck::getTitle)
            .map(String::trim)
            .toList();
    assertTrue(
        primaryCheckTitles.contains(
            "The HTTP request has valid content (conditional validation rules)"));
    assertFalse(
        primaryCheckTitles.contains(
            "The HTTP response has valid content (conditional validation rules)"));
  }
}

