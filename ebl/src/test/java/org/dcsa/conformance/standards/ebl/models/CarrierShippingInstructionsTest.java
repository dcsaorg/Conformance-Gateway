package org.dcsa.conformance.standards.ebl.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.ebl.party.AmendedTransportDocumentStatus;
import org.junit.jupiter.api.Test;

class CarrierShippingInstructionsTest {

  @Test
  void directAmendmentPreservesOriginalAndCanBeCancelled() {
    CarrierShippingInstructions model = model("DRAFT");
    ObjectNode amendment = amendment("DRAFT");

    model.receiveDirectTransportDocumentAmendment("TDR-1", amendment);

    assertEquals("ORIGINAL", model.getTransportDocument().orElseThrow().path("serviceContractReference").asText());
    assertEquals("AMENDED", model.getAmendedTransportDocument().orElseThrow().path("serviceContractReference").asText());
    assertNotSame(amendment, model.getAmendedTransportDocument().orElseThrow());
    assertEquals(
        AmendedTransportDocumentStatus.AMENDMENT_RECEIVED,
        model.getAmendedTransportDocumentStatus().orElseThrow());

    model.cancelDirectTransportDocumentAmendment("TDR-1");

    assertEquals(
        AmendedTransportDocumentStatus.AMENDMENT_CANCELLED,
        model.getAmendedTransportDocumentStatus().orElseThrow());
    assertEquals("DRAFT", model.getTransportDocumentState().wireName());
  }

  @Test
  void directAmendmentCanBeConfirmedOrDeclinedWithoutChangingPrimaryStatus() {
    CarrierShippingInstructions confirmed = model("ISSUED");
    confirmed.receiveDirectTransportDocumentAmendment("TDR-1", amendment("ISSUED"));
    confirmed.processDirectTransportDocumentAmendment("TDR-1", true);
    assertEquals(
        AmendedTransportDocumentStatus.AMENDMENT_CONFIRMED,
        confirmed.getAmendedTransportDocumentStatus().orElseThrow());
    assertEquals("ISSUED", confirmed.getTransportDocumentState().wireName());

    CarrierShippingInstructions declined = model("PENDING_SURRENDER_FOR_AMENDMENT");
    declined.receiveDirectTransportDocumentAmendment(
        "TDR-1", amendment("PENDING_SURRENDER_FOR_AMENDMENT"));
    declined.processDirectTransportDocumentAmendment("TDR-1", false);
    assertEquals(
        AmendedTransportDocumentStatus.AMENDMENT_DECLINED,
        declined.getAmendedTransportDocumentStatus().orElseThrow());
    assertEquals(
        "PENDING_SURRENDER_FOR_AMENDMENT", declined.getTransportDocumentState().wireName());
  }

  @Test
  void directAmendmentRejectsMismatchedReferenceStatusAndInvalidTransitions() {
    CarrierShippingInstructions model = model("DRAFT");

    assertThrows(
        IllegalStateException.class,
        () -> model.receiveDirectTransportDocumentAmendment("TDR-1", amendment("ISSUED")));
    ObjectNode wrongReference = amendment("DRAFT").put("transportDocumentReference", "TDR-2");
    assertThrows(
        IllegalStateException.class,
        () -> model.receiveDirectTransportDocumentAmendment("TDR-1", wrongReference));
    assertThrows(
        IllegalStateException.class,
        () -> model.cancelDirectTransportDocumentAmendment("TDR-1"));
  }

  private CarrierShippingInstructions model(String status) {
    ObjectNode original = amendment(status).put("serviceContractReference", "ORIGINAL");
    return CarrierShippingInstructions.initializeFromTransportDocument(original, "3.0.0");
  }

  private ObjectNode amendment(String status) {
    return JsonToolkit.OBJECT_MAPPER
        .createObjectNode()
        .put("transportDocumentReference", "TDR-1")
        .put("shippingInstructionsReference", "SIR-1")
        .put("transportDocumentStatus", status)
        .put("serviceContractReference", "AMENDED");
  }
}


