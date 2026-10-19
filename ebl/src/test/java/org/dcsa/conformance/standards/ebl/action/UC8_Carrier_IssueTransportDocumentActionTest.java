package org.dcsa.conformance.standards.ebl.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.standards.ebl.checks.CarrierTdNotificationPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.junit.jupiter.api.Test;

class UC8_Carrier_IssueTransportDocumentActionTest {
  @Test
  void standaloneIssuancePreservesSuppliedReferenceAcrossResetImportAndNotification() {
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
      assertEquals("ISSUED-TDR", action.asJsonNode().required("documentReference").asText());
      assertEquals(type.name(), action.getDSP().scenarioType());
      assertTrue(action.getDSP().newTransportDocumentContent());
    }
  }

  @Test
  void standaloneIssuanceLearnsReferenceOnlyWhenNoneIsKnown() {
    var action =
        new UC8_Carrier_IssueTransportDocumentAction(
            "Carrier", "Shipper", null, null, true, ScenarioType.REGULAR_SWB);
    action.handleExchange(notification("NOTIFIED-TDR"));
    assertEquals("NOTIFIED-TDR", action.getTransportDocumentReference());
    action.handleExchange(notification("DIFFERENT-TDR"));
    assertEquals("NOTIFIED-TDR", action.getTransportDocumentReference());
  }

  @Test
  void notificationMismatchFailsBothReferenceChecksRegardlessOfInputOrder() {
    for (boolean inputFirst : List.of(false, true)) {
      var action =
          new UC8_Carrier_IssueTransportDocumentAction(
              "Carrier", "Shipper", null, null, true, ScenarioType.REGULAR_SWB);
      var input = OBJECT_MAPPER.createObjectNode();
      input.putObject("input").put("transportDocumentReference", "SUPPLIED-TDR");
      var exchange = notification("NOTIFIED-TDR");
      if (inputFirst) action.doHandlePartyInput(input);
      action.handleExchange(exchange);
      if (!inputFirst) action.doHandlePartyInput(input);

      assertEquals("SUPPLIED-TDR", action.getTransportDocumentReference());
      var payloadCheck =
          action
              .createCheck("3.0.0")
              .subChecksStream()
              .filter(CarrierTdNotificationPayloadRequestConformanceCheck.class::isInstance)
              .findFirst()
              .orElseThrow();
      var referenceChecks =
          flattenedChecks(payloadCheck)
              .filter(c -> c.getTitle().contains("correct transportDocumentReference"))
              .toList();
      assertEquals(2, referenceChecks.size());
      referenceChecks.forEach(
          check -> {
            check.check(id -> exchange);
            var result = check.resultsStream().findFirst().orElseThrow();
            assertFalse(result.isConformant());
            assertTrue(
                result.getErrors().stream()
                    .anyMatch(
                        error -> error.contains("NOTIFIED-TDR") && error.contains("SUPPLIED-TDR")));
          });
      var retrieval =
          new Shipper_GetTransportDocumentAction(
              "Carrier",
              "Shipper",
              action,
              List.of(org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_ISSUED),
              null);
      assertEquals("SUPPLIED-TDR", retrieval.getTransportDocumentReference());
    }
  }

  private static Stream<ConformanceCheck> flattenedChecks(ConformanceCheck check) {
    return Stream.concat(
        Stream.of(check),
        check
            .subChecksStream()
            .flatMap(UC8_Carrier_IssueTransportDocumentActionTest::flattenedChecks));
  }

  private static ConformanceExchange notification(String reference) {
    var body = OBJECT_MAPPER.createObjectNode();
    var data = body.putObject("data");
    data.put("transportDocumentReference", reference);
    data.putObject("transportDocument").put("transportDocumentReference", reference);
    var request = request(body);
    return new ConformanceExchange(
        request, request.createResponse(204, Map.of(), new ConformanceMessageBody("")));
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
