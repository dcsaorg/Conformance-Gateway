package org.dcsa.conformance.standards.adoption.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.UserFacingException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SupplyScenarioParametersActionTest {

  private final SupplyScenarioParametersAction action = new SupplyScenarioParametersAction("publisherPartyName");

  @ParameterizedTest
  @CsvSource({
    "2024-12-31,week",
    "2024-12-31,month",
    "2024-12-31,day",
    "2024-01-01,week"
  })
  void testHandlePartyInput(String date, String interval) {
    JsonNode inputNode = OBJECT_MAPPER.createObjectNode().set("input", OBJECT_MAPPER.createObjectNode().put("date", date).put("interval", interval));
    action.handlePartyInput(inputNode);
    String output = action.getSuppliedScenarioParameters().toJson().toString();
    assertTrue(output.contains("\"interval\":\"%s\"".formatted(interval)));
    assertTrue(output.contains("\"date\":\"%s\"".formatted(date)));
  }

  @ParameterizedTest
  @CsvSource({
    "01-01-2024,week",
    "2024-12-32,week",
    "2024-12-31,monthly",
    "2024-12-31,quarter",
    "2024-12-31,year",
    ","
  })
  void testHandlePartyInputWrongDataShouldThrowException(String date, String interval) {
    JsonNode inputNode = OBJECT_MAPPER.createObjectNode().set("input", OBJECT_MAPPER.createObjectNode().put("date", date).put("interval", interval));
    assertThrows(UserFacingException.class, () -> action.handlePartyInput(inputNode));
  }
}
