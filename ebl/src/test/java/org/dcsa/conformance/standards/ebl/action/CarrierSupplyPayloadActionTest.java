package org.dcsa.conformance.standards.ebl.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.ebl.checks.EblInputPayloadValidations;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standardscommons.party.EblDynamicScenarioParameters;
import org.junit.jupiter.api.Test;

class CarrierSupplyPayloadActionTest {

  private final CarrierSupplyPayloadAction action =
      new CarrierSupplyPayloadAction(
          "Carrier", ScenarioType.REGULAR_STRAIGHT_BL, "3.0.0", null, true, true);

  @Test
  void infersEverySupportedTransportDocumentScope() throws Exception {
    assertEquals(
        ScenarioType.REGULAR_SWB,
        action.inputScenarioType(amendedTd("SWB", false, "TDR-1", "DRAFT")));
    assertEquals(
        ScenarioType.REGULAR_STRAIGHT_BL,
        action.inputScenarioType(amendedTd("BOL", false, "TDR-1", "DRAFT")));
    assertEquals(
        ScenarioType.REGULAR_NEGOTIABLE_BL,
        action.inputScenarioType(amendedTd("BOL", true, "TDR-1", "DRAFT")));
  }

  @Test
  void validatesAllowedAmendedTransportDocumentStatuses() throws Exception {
    for (String status : Set.of("DRAFT", "ISSUED", "PENDING_SURRENDER_FOR_AMENDMENT")) {
      assertTrue(
          action
              .validateAmendedTransportDocument(amendedTd("BOL", false, "TDR-1", status))
              .isEmpty());
    }
    assertTrue(
        action
            .validateAmendedTransportDocument(amendedTd("BOL", false, "TDR-1", "APPROVED"))
            .stream()
            .anyMatch(error -> error.contains("must equal `DRAFT`, `ISSUED`")));
  }

  @Test
  void missingOrNullInputProducesAUserFacingError() {
    UserFacingException missingInputError =
        assertThrows(
            UserFacingException.class,
            () -> action.handlePartyInput(OBJECT_MAPPER.createObjectNode()));
    UserFacingException nullInputError =
        assertThrows(
            UserFacingException.class,
            () ->
                action.handlePartyInput(
                    OBJECT_MAPPER.createObjectNode().putNull("input")));

    assertEquals(
        "The party input must contain a non-null 'input' value.", missingInputError.getMessage());
    assertEquals(missingInputError.getMessage(), nullInputError.getMessage());
  }

  @Test
  void generatedTransportDocumentsMatchLatestSchemaAndWorkbookRules() {
    JsonSchemaValidator schemaValidator =
        JsonSchemaValidator.getInstance(
            "/standards/ebl/schemas/EBL_v3.0.0.yaml", "TransportDocument");

    for (ScenarioType scenarioType :
        Set.of(
            ScenarioType.REGULAR_SWB,
            ScenarioType.REGULAR_STRAIGHT_BL,
            ScenarioType.REGULAR_NEGOTIABLE_BL)) {
      CarrierSupplyPayloadAction supplyAction =
          new CarrierSupplyPayloadAction("Carrier", scenarioType, "3.0.0", schemaValidator, true);
      JsonNode transportDocument = supplyAction.getJsonForHumanReadablePrompt();
      var dsp =
          new EblDynamicScenarioParameters(
              scenarioType.name(),
              null,
              transportDocument.required("transportDocumentReference").asText(),
              transportDocument.required("transportDocumentStatus").asText(),
              null,
              null,
              false,
              false);

      assertTrue(schemaValidator.validate(transportDocument).isEmpty(), scenarioType.name());
      assertTrue(
          EblInputPayloadValidations.validateEblContent(
                  transportDocument, scenarioType, true, dsp)
              .isEmpty(),
          scenarioType.name());
    }
  }

  @Test
  void generatedAmendmentsCoverEverySupportedTransportDocumentScope() {
    JsonSchemaValidator schemaValidator =
        JsonSchemaValidator.getInstance(
            "/standards/ebl/schemas/EBL_v3.0.0.yaml", "TransportDocument");

    for (ScenarioType scenarioType :
        Set.of(
            ScenarioType.REGULAR_SWB,
            ScenarioType.REGULAR_STRAIGHT_BL,
            ScenarioType.REGULAR_NEGOTIABLE_BL)) {
      CarrierSupplyPayloadAction supplyAction =
          new CarrierSupplyPayloadAction(
              "Carrier", scenarioType, "3.0.0", schemaValidator, true, true);
      JsonNode amendedTransportDocument = supplyAction.getJsonForHumanReadablePrompt();
      var dsp =
          new EblDynamicScenarioParameters(
              scenarioType.name(),
              null,
              amendedTransportDocument.required("transportDocumentReference").asText(),
              amendedTransportDocument.required("transportDocumentStatus").asText(),
              null,
              null,
              false,
              false);
      assertTrue(schemaValidator.validate(amendedTransportDocument).isEmpty(), scenarioType.name());
      assertTrue(
          EblInputPayloadValidations.validateEblContent(
                  amendedTransportDocument, scenarioType, true, dsp)
              .isEmpty(),
          scenarioType.name());
      assertTrue(
          supplyAction.validateAmendedTransportDocument(amendedTransportDocument).isEmpty(),
          scenarioType.name());
      ObjectNode partyInput = OBJECT_MAPPER.createObjectNode();
      partyInput.set("input", amendedTransportDocument);
      supplyAction.handlePartyInput(partyInput);
      assertEquals(
          amendedTransportDocument.required("transportDocumentStatus").asText(),
          supplyAction.getDSP().transportDocumentStatus(),
          scenarioType.name());
      assertEquals(
          amendedTransportDocument.required("transportDocumentReference").asText(),
          supplyAction.getDSP().transportDocumentReference(),
          scenarioType.name());
    }
  }

  @Test
  void carrierSuppliedTransportDocumentMustMatchSelectedScope() {
    CarrierSupplyPayloadAction supplyAction =
        new CarrierSupplyPayloadAction(
            "Carrier", ScenarioType.REGULAR_STRAIGHT_BL, "3.0.0", null, true);
    ObjectNode transportDocument =
        (ObjectNode) supplyAction.getJsonForHumanReadablePrompt().deepCopy();
    transportDocument.put("isToOrder", true);
    var dsp =
        new EblDynamicScenarioParameters(
            ScenarioType.REGULAR_STRAIGHT_BL.name(),
            null,
            transportDocument.required("transportDocumentReference").asText(),
            transportDocument.required("transportDocumentStatus").asText(),
            null,
            null,
            false,
            false);

    assertFalse(
        EblInputPayloadValidations.validateEblContent(
                transportDocument, ScenarioType.REGULAR_STRAIGHT_BL, true, dsp)
            .isEmpty());
  }

  private static JsonNode amendedTd(
      String type, boolean isToOrder, String reference, String status)
      throws Exception {
    return OBJECT_MAPPER.readTree(
        """
        {
          "transportDocumentTypeCode": "%s",
          "isToOrder": %s,
          "transportDocumentReference": "%s",
          "transportDocumentStatus": "%s"
        }
        """
            .formatted(type, isToOrder, reference, status));
  }
}

