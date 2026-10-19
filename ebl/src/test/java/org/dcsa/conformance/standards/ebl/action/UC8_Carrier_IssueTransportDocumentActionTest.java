package org.dcsa.conformance.standards.ebl.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.junit.jupiter.api.Test;

class UC8_Carrier_IssueTransportDocumentActionTest {
  @Test
  void standaloneIssuanceAcceptsInputWithoutNotificationsAndLearnsNotificationReference() {
    for (ScenarioType type :
        List.of(
            ScenarioType.REGULAR_SWB,
            ScenarioType.REGULAR_STRAIGHT_BL,
            ScenarioType.REGULAR_NEGOTIABLE_BL)) {
      var action =
          new UC8_Carrier_IssueTransportDocumentAction(
              "Carrier", "Shipper", null, null, false, type);
      var input = OBJECT_MAPPER.createObjectNode();
      input.putObject("input").put("transportDocumentReference", "ISSUED-TDR");
      action.doHandlePartyInput(input);
      assertEquals("ISSUED-TDR", action.asJsonNode().required("documentReference").asText());
      var state = action.exportJsonState();
      action.reset();
      assertTrue(action.asJsonNode().path("documentReference").isNull());
      action.importJsonState(state);
      assertEquals("ISSUED-TDR", action.asJsonNode().required("documentReference").asText());
      var body = OBJECT_MAPPER.createObjectNode();
      body.putObject("data").put("transportDocumentReference", "NOTIFIED-TDR");
      var request = request(body);
      action.doHandleExchange(
          new ConformanceExchange(
              request,
              request.createResponse(
                  204, Map.of(), new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()))));
      assertEquals("NOTIFIED-TDR", action.asJsonNode().required("documentReference").asText());
      assertEquals(type.name(), action.getDSP().scenarioType());
      assertTrue(action.getDSP().newTransportDocumentContent());
    }
  }

  private static ConformanceRequest request(com.fasterxml.jackson.databind.JsonNode body) {
    return new ConformanceRequest(
        "POST",
        "http://localhost/v3/transport-document-notifications",
        Map.of(),
        new ConformanceMessage(
            "Carrier",
            "Carrier",
            "Shipper",
            "Shipper",
            Map.of(),
            new ConformanceMessageBody(body),
            0));
  }
}
