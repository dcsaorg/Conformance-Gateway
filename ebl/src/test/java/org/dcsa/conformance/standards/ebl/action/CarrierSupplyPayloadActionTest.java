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
            "transportDocumentReference": "%s"
          },
          "amendedTransportDocument": {
            "transportDocumentTypeCode": "%s",
            "isToOrder": %s,
            "transportDocumentReference": "%s"
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

