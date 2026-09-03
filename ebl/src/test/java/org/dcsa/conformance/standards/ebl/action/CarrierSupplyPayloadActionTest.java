package org.dcsa.conformance.standards.ebl.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
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
    assertTrue(
        action
            .validateAmendmentPair(
                tdPair("BOL", false, "TDR-1", "BOL", false, "TDR-1"))
            .isEmpty());

    Set<String> errors =
        action.validateAmendmentPair(
            tdPair("SWB", false, "TDR-1", "BOL", true, "TDR-2"));
    assertEquals(3, errors.size());
    assertTrue(errors.stream().anyMatch(error -> error.contains("transportDocumentTypeCode")));
    assertTrue(errors.stream().anyMatch(error -> error.contains("isToOrder")));
    assertTrue(errors.stream().anyMatch(error -> error.contains("transportDocumentReference")));
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

