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
        action.inputScenarioType(tdPair("SWB", false, "TDR-1", "SWB", false, "TDR-1")));
    assertEquals(
        ScenarioType.REGULAR_STRAIGHT_BL,
        action.inputScenarioType(tdPair("BOL", false, "TDR-1", "BOL", false, "TDR-1")));
    assertEquals(
        ScenarioType.REGULAR_NEGOTIABLE_BL,
        action.inputScenarioType(tdPair("BOL", true, "TDR-1", "BOL", true, "TDR-1")));
  }

  @Test
  void validatesThatAnAmendmentRetainsTheOriginalDocumentIdentity() throws Exception {
    JsonNode validPair = tdPair("BOL", false, "TDR-1", "BOL", false, "TDR-1");
    ((ObjectNode) validPair.required("amendedTransportDocument"))
        .put("serviceContractReference", "AMENDED-SERVICE-CONTRACT");
    assertTrue(action.validateAmendmentPair(validPair).isEmpty());

    Set<String> errors =
        action.validateAmendmentPair(
            tdPair("SWB", false, "TDR-1", "BOL", true, "TDR-2"));
    assertEquals(3, errors.size());
    assertTrue(errors.stream().anyMatch(error -> error.contains("transportDocumentTypeCode")));
    assertTrue(errors.stream().anyMatch(error -> error.contains("isToOrder")));
    assertTrue(errors.stream().anyMatch(error -> error.contains("transportDocumentReference")));
  }

  @Test
  void validatesThatAnAmendmentRetainsAnAllowedOriginalStatus() throws Exception {
    ObjectNode changedStatusPair =
        (ObjectNode) tdPair("BOL", false, "TDR-1", "BOL", false, "TDR-1");
    ((ObjectNode) changedStatusPair.required("amendedTransportDocument"))
        .put("transportDocumentStatus", "ISSUED");
    assertTrue(
        action.validateAmendmentPair(changedStatusPair).stream()
            .anyMatch(error -> error.contains("same `transportDocumentStatus`")));

    ObjectNode invalidStatusPair = changedStatusPair.deepCopy();
    ((ObjectNode) invalidStatusPair.required("transportDocument"))
        .put("transportDocumentStatus", "APPROVED");
    assertTrue(
        action.validateAmendmentPair(invalidStatusPair).stream()
            .anyMatch(error -> error.contains("must equal `DRAFT`, `ISSUED`")));
  }

  @Test
  void rejectsAnAmendmentWithoutAnyChangedValue() throws Exception {
    Set<String> errors =
        action.validateAmendmentPair(
            tdPair("BOL", false, "TDR-1", "BOL", false, "TDR-1"));

    assertEquals(Set.of("The amended Transport Document must differ from the original."), errors);
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
      JsonNode pair = supplyAction.getJsonForHumanReadablePrompt();

      for (String field : Set.of("transportDocument", "amendedTransportDocument")) {
        JsonNode transportDocument = pair.required(field);
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
      assertTrue(supplyAction.validateAmendmentPair(pair).isEmpty(), scenarioType.name());
      ObjectNode partyInput = OBJECT_MAPPER.createObjectNode();
      partyInput.set("input", pair);
      supplyAction.handlePartyInput(partyInput);
      assertEquals(
          pair.required("transportDocument").required("transportDocumentStatus").asText(),
          supplyAction.getDSP().transportDocumentStatus(),
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

  private static JsonNode tdPair(
      String originalType,
      boolean originalIsToOrder,
      String originalReference,
      String amendedType,
      boolean amendedIsToOrder,
      String amendedReference)
      throws Exception {
    return OBJECT_MAPPER.readTree(
        """
        {
          "transportDocument": {
            "transportDocumentTypeCode": "%s",
            "isToOrder": %s,
            "transportDocumentReference": "%s",
            "transportDocumentStatus": "DRAFT"
          },
          "amendedTransportDocument": {
            "transportDocumentTypeCode": "%s",
            "isToOrder": %s,
            "transportDocumentReference": "%s",
            "transportDocumentStatus": "DRAFT"
          }
        }
        """
            .formatted(
                originalType,
                originalIsToOrder,
                originalReference,
                amendedType,
                amendedIsToOrder,
                amendedReference));
  }
}

