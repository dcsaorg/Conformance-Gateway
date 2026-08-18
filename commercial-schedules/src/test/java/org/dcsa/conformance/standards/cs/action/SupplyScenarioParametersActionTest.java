package org.dcsa.conformance.standards.cs.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
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

