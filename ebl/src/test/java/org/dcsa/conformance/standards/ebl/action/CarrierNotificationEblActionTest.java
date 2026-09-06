package org.dcsa.conformance.standards.ebl.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CarrierNotificationEblActionTest {

  @Test
  void everyCarrierNotificationActionIsOptionalForTheCarrier() {
    List<Class<?>> carrierNotificationActions =
        List.of(
            UC2_Carrier_RequestUpdateToShippingInstructionsAction.class,
            UC4_Carrier_ProcessUpdateToShippingInstructionsAction.class,
            UC6_Carrier_PublishDraftTransportDocumentAction.class,
            UC8_Carrier_IssueTransportDocumentAction.class,
            UC9_Carrier_AwaitSurrenderRequestForAmendmentAction.class,
            UC10_Carrier_ProcessSurrenderRequestForAmendmentAction.class,
            UC11_Carrier_voidTDAndIssueAmendedTransportDocumentAction.class,
            UC12_Carrier_AwaitSurrenderRequestForDeliveryAction.class,
            UC13_Carrier_ProcessSurrenderRequestForDeliveryAction.class,
            UC14_Carrier_ConfirmShippingInstructionsCompleteAction.class,
            UC16_Carrier_DeclineShippingInstructionsAction.class,
            UC19_Carrier_ProcessTransportDocumentAmendmentAction.class,
            UCX_Carrier_TDOnlyProcessOutOfBandUpdateOrAmendmentRequestDraftTransportDocumentAction
                .class);

    carrierNotificationActions.forEach(
        actionClass ->
            assertTrue(
                CarrierNotificationEblAction.class.isAssignableFrom(actionClass),
                actionClass.getSimpleName()));

    var action =
        new UC19_Carrier_ProcessTransportDocumentAmendmentAction(
            "Carrier", "Shipper", null, null, true, false);
    assertEquals(Set.of("Carrier"), action.completableWithoutTrafficForRoles());
  }
}

