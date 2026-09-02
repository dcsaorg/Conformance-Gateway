package org.dcsa.conformance.standards.cs.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.standards.cs.party.CsFilterParameter;
import org.junit.jupiter.api.Test;

class SupplyScenarioParametersActionTest {

  private final SupplyScenarioParametersAction action =
      new SupplyScenarioParametersAction("publisherPartyName", CsFilterParameter.DATE);

  @Test
  void testHandlePartyInput_validInputIsAccepted() {
    ObjectNode partyInput = OBJECT_MAPPER.createObjectNode();
    partyInput.set("input", OBJECT_MAPPER.createObjectNode().put("date", "2026-08-18"));

    action.handlePartyInput(partyInput);

    assertTrue(
        action.getSuppliedScenarioParameters().toJson().has("date"),
        "Expected supplied scenario parameters to be stored");
    assertEquals(
        "2026-08-18",
        action.getSuppliedScenarioParameters().toJson().get("date").asText());
  }

  @Test
  void testGetExpectedInputAttributes_marksOptionalParametersAsOptional() {
    SupplyScenarioParametersAction actionWithOptionalParameters =
        new SupplyScenarioParametersAction(
            "publisherPartyName",
            new CsFilterParameter[] {
              CsFilterParameter.PLACE_OF_RECEIPT, CsFilterParameter.PLACE_OF_DELIVERY
            },
            new CsFilterParameter[] {
              CsFilterParameter.DEPARTURE_START_DATE,
              CsFilterParameter.DEPARTURE_END_DATE,
              CsFilterParameter.ARRIVAL_START_DATE,
              CsFilterParameter.ARRIVAL_END_DATE,
              CsFilterParameter.MAX_TRANSHIPMENT,
              CsFilterParameter.RECEIPT_TYPE_AT_ORIGIN,
              CsFilterParameter.DELIVERY_TYPE_AT_DESTINATION,
              CsFilterParameter.CARGO_TYPE
            });

    Map<String, Boolean> expectedInputAttributes =
        actionWithOptionalParameters.getExpectedInputAttributes();

    assertEquals(Boolean.TRUE, expectedInputAttributes.get("placeOfReceipt"));
    assertEquals(Boolean.TRUE, expectedInputAttributes.get("placeOfDelivery"));
    assertEquals(Boolean.FALSE, expectedInputAttributes.get("departureStartDate"));
    assertEquals(Boolean.FALSE, expectedInputAttributes.get("cargoType"));
  }

  @Test
  void testHandlePartyInput_optionalScenario_acceptsOnlyRequiredParameters() {
    SupplyScenarioParametersAction actionWithOptionalParameters =
        new SupplyScenarioParametersAction(
            "publisherPartyName",
            new CsFilterParameter[] {
              CsFilterParameter.PLACE_OF_RECEIPT, CsFilterParameter.PLACE_OF_DELIVERY
            },
            new CsFilterParameter[] {
              CsFilterParameter.DEPARTURE_START_DATE,
              CsFilterParameter.DEPARTURE_END_DATE,
              CsFilterParameter.ARRIVAL_START_DATE,
              CsFilterParameter.ARRIVAL_END_DATE,
              CsFilterParameter.MAX_TRANSHIPMENT,
              CsFilterParameter.RECEIPT_TYPE_AT_ORIGIN,
              CsFilterParameter.DELIVERY_TYPE_AT_DESTINATION,
              CsFilterParameter.CARGO_TYPE
            });
    ObjectNode partyInput = OBJECT_MAPPER.createObjectNode();
    partyInput.set(
        "input",
        OBJECT_MAPPER
            .createObjectNode()
            .put("placeOfReceipt", "NLRTM")
            .put("placeOfDelivery", "DKCPH"));

    actionWithOptionalParameters.handlePartyInput(partyInput);

    JsonNode stored = actionWithOptionalParameters.getSuppliedScenarioParameters().toJson();
    assertEquals("NLRTM", stored.path("placeOfReceipt").asText());
    assertEquals("DKCPH", stored.path("placeOfDelivery").asText());
    assertFalse(stored.has("departureStartDate"));
  }

  @Test
  void testHandlePartyInput_optionalScenario_missingMandatoryParameterFails() {
    SupplyScenarioParametersAction actionWithOptionalParameters =
        new SupplyScenarioParametersAction(
            "publisherPartyName",
            new CsFilterParameter[] {
              CsFilterParameter.PLACE_OF_RECEIPT, CsFilterParameter.PLACE_OF_DELIVERY
            },
            new CsFilterParameter[] {
              CsFilterParameter.DEPARTURE_START_DATE,
              CsFilterParameter.DEPARTURE_END_DATE,
              CsFilterParameter.ARRIVAL_START_DATE,
              CsFilterParameter.ARRIVAL_END_DATE,
              CsFilterParameter.MAX_TRANSHIPMENT,
              CsFilterParameter.RECEIPT_TYPE_AT_ORIGIN,
              CsFilterParameter.DELIVERY_TYPE_AT_DESTINATION,
              CsFilterParameter.CARGO_TYPE
            });
    ObjectNode partyInput = OBJECT_MAPPER.createObjectNode();
    partyInput.set("input", OBJECT_MAPPER.createObjectNode().put("placeOfReceipt", "NLRTM"));

    UserFacingException exception =
        assertThrows(
            UserFacingException.class,
            () -> actionWithOptionalParameters.handlePartyInput(partyInput));

    assertTrue(exception.getMessage().contains("placeOfDelivery"));
  }

  @Test
  void testHandlePartyInput_optionalScenario_blankOptionalParameterFails() {
    SupplyScenarioParametersAction actionWithOptionalParameters =
        new SupplyScenarioParametersAction(
            "publisherPartyName",
            new CsFilterParameter[] {
              CsFilterParameter.PLACE_OF_RECEIPT, CsFilterParameter.PLACE_OF_DELIVERY
            },
            new CsFilterParameter[] {
              CsFilterParameter.DEPARTURE_START_DATE,
              CsFilterParameter.DEPARTURE_END_DATE,
              CsFilterParameter.ARRIVAL_START_DATE,
              CsFilterParameter.ARRIVAL_END_DATE,
              CsFilterParameter.MAX_TRANSHIPMENT,
              CsFilterParameter.RECEIPT_TYPE_AT_ORIGIN,
              CsFilterParameter.DELIVERY_TYPE_AT_DESTINATION,
              CsFilterParameter.CARGO_TYPE
            });
    ObjectNode partyInput = OBJECT_MAPPER.createObjectNode();
    partyInput.set(
        "input",
        OBJECT_MAPPER
            .createObjectNode()
            .put("placeOfReceipt", "NLRTM")
            .put("placeOfDelivery", "DKCPH")
            .put("cargoType", " "));

    UserFacingException exception =
        assertThrows(
            UserFacingException.class,
            () -> actionWithOptionalParameters.handlePartyInput(partyInput));

    assertTrue(exception.getMessage().contains("cargoType"));
  }

  @Test
  void testGetHumanReadablePrompt_optionalScenarioListsMandatoryAndOptionalParameters() {
    SupplyScenarioParametersAction actionWithOptionalParameters =
        new SupplyScenarioParametersAction(
            "publisherPartyName",
            new CsFilterParameter[] {
              CsFilterParameter.PLACE_OF_RECEIPT, CsFilterParameter.PLACE_OF_DELIVERY
            },
            new CsFilterParameter[] {
              CsFilterParameter.DEPARTURE_START_DATE,
              CsFilterParameter.DEPARTURE_END_DATE,
              CsFilterParameter.ARRIVAL_START_DATE,
              CsFilterParameter.ARRIVAL_END_DATE,
              CsFilterParameter.MAX_TRANSHIPMENT,
              CsFilterParameter.RECEIPT_TYPE_AT_ORIGIN,
              CsFilterParameter.DELIVERY_TYPE_AT_DESTINATION,
              CsFilterParameter.CARGO_TYPE
            });

    String prompt = actionWithOptionalParameters.getHumanReadablePrompt();

    assertTrue(prompt.contains("Mandatory query parameters"));
    assertTrue(prompt.contains("`placeOfReceipt`"));
    assertTrue(prompt.contains("`placeOfDelivery`"));
    assertTrue(prompt.contains("Optional query parameters"));
    assertTrue(prompt.contains("`departureStartDate`"));
    assertTrue(prompt.contains("`cargoType`"));
  }

  @Test
  void testGetJsonForHumanReadablePrompt_ordersRequiredBeforeOptional() {
    SupplyScenarioParametersAction actionWithOptionalParameters =
        new SupplyScenarioParametersAction(
            "publisherPartyName",
            new CsFilterParameter[] {
              CsFilterParameter.PLACE_OF_RECEIPT, CsFilterParameter.PLACE_OF_DELIVERY
            },
            new CsFilterParameter[] {
              CsFilterParameter.DEPARTURE_START_DATE,
              CsFilterParameter.CARGO_TYPE
            });

    JsonNode promptJson = actionWithOptionalParameters.getJsonForHumanReadablePrompt();

    assertInstanceOf(ObjectNode.class, promptJson);
    assertNotNull(promptJson.get("placeOfReceipt"));
    assertNotNull(promptJson.get("placeOfDelivery"));
    assertNotNull(promptJson.get("departureStartDate"));
    assertNotNull(promptJson.get("cargoType"));
  }

  @Test
  void testHandlePartyInput_missingInputObjectThrowsUserFacingException() {
    ObjectNode partyInput = OBJECT_MAPPER.createObjectNode();

    UserFacingException exception =
        assertThrows(UserFacingException.class, () -> action.handlePartyInput(partyInput));

    assertTrue(exception.getMessage().contains("input"));
  }

  @Test
  void testHandlePartyInput_nullInputObjectThrowsUserFacingException() {
    ObjectNode partyInput = OBJECT_MAPPER.createObjectNode();
    partyInput.set("input", OBJECT_MAPPER.nullNode());

    UserFacingException exception =
        assertThrows(UserFacingException.class, () -> action.handlePartyInput(partyInput));

    assertTrue(exception.getMessage().contains("input"));
  }
}

