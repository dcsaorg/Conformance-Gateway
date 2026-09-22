package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.an.checks.ScenarioType;
import org.dcsa.conformance.standards.an.party.ANFilterParameter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplyScenarioParametersActionTest {

  @Test
  void rejectsInvalidLimitValues() {
    SupplyScenarioParametersAction action = paginationInputAction();

    assertThrows(UserFacingException.class, () -> action.handlePartyInput(partyInput("abc", true)));
    assertThrows(UserFacingException.class, () -> action.handlePartyInput(partyInput("0", true)));
    assertThrows(UserFacingException.class, () -> action.handlePartyInput(partyInput("-1", true)));
  }

  @Test
  void rejectsNonTextValuesInsideArrayFilters() {
    SupplyScenarioParametersAction action = paginationInputAction();
    ObjectNode input = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    ObjectNode body = input.putObject("input");
    ArrayNode references = body.putArray("transportDocumentReferences");
    references.add("HHL71800000");
    references.addObject().put("bad", "value");
    body.put("limit", 1);

    assertThrows(UserFacingException.class, () -> action.handlePartyInput(input));
  }

  @Test
  void rejectsUnknownParametersWhenNoTemplateIsProvided() {
    SupplyScenarioParametersAction action =
      new SupplyScenarioParametersAction(
        "Publisher1",
        ScenarioType.BASIC,
        new ANFilterParameter[] {},
        new ANFilterParameter[] {},
        "Supply Parameters");

    ObjectNode input = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    input.putObject("input").put("unsupportedFilter", "value");

    assertThrows(UserFacingException.class, () -> action.handlePartyInput(input));
  }

  @Test
  void acceptsValidLimitAndStoresSuppliedParameters() {
    SupplyScenarioParametersAction action = paginationInputAction();

    action.handlePartyInput(partyInput("1", true));

    assertEquals(
      "1",
      action.exportJsonState().path("currentDsp").path("suppliedQueryParameters").path("limit").asText());
  }

  private static SupplyScenarioParametersAction paginationInputAction() {
    return new SupplyScenarioParametersAction(
      "Publisher1",
      ScenarioType.BASIC,
      new ANFilterParameter[] {
        ANFilterParameter.TRANSPORT_DOCUMENT_REFERENCES,
        ANFilterParameter.LIMIT
      },
      new ANFilterParameter[] {},
      "Supply Parameters");
  }

  private static ObjectNode partyInput(String limit, boolean useArrayForTransportDocumentReferences) {
    ObjectNode input = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    ObjectNode body = input.putObject("input");
    if (useArrayForTransportDocumentReferences) {
      body.putArray("transportDocumentReferences").add("HHL71800000");
    } else {
      body.put("transportDocumentReferences", "HHL71800000");
    }
    body.put("limit", limit);
    return input;
  }
}

