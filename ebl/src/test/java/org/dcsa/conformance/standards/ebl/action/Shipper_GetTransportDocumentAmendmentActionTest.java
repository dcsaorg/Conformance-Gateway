package org.dcsa.conformance.standards.ebl.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceResult;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.AmendedTransportDocumentStatus;
import org.junit.jupiter.api.Test;

class Shipper_GetTransportDocumentAmendmentActionTest {
  @Test
  void amendmentRetrievalUsesTheDocumentedResponseClassForEachRole() {
    var previous =
        new UC8_Carrier_IssueTransportDocumentAction(
            "Carrier", "Shipper", null, null, false, ScenarioType.REGULAR_SWB);
    for (boolean carrierScenario : List.of(false, true)) {
      for (int status : List.of(199, 200, 201, 202, 204, 206, 299, 300, 400)) {
        var action =
            new Shipper_GetTransportDocumentAmendmentAction(
                "Carrier",
                "Shipper",
                previous,
                null,
                AmendedTransportDocumentStatus.AMENDMENT_RECEIVED,
                carrierScenario);
        var check =
            action
                .createCheck("3.0.0")
                .subChecksStream()
                .filter(ResponseStatusCheck.class::isInstance)
                .findFirst()
                .orElseThrow();
        var body = new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode());
        var request =
            new ConformanceRequest(
                "GET",
                "http://localhost/v3/transport-document-amendments/TDR",
                Map.of(),
                new ConformanceMessage(
                    "Shipper", "Shipper", "Carrier", "Carrier", Map.of(), body, 0));
        var exchange =
            new ConformanceExchange(request, request.createResponse(status, Map.of(), body));
        check.check(id -> exchange);
        assertEquals(1, check.resultsStream().count());
        assertEquals(
            carrierScenario ? status >= 200 && status < 300 : status == 200,
            check.resultsStream().allMatch(ConformanceResult::isConformant));
      }
    }
  }

  @Test
  void amendmentRetrievalEnforcesPayloadValidationAndBodyless202() throws Exception {
    var schema =
        JsonSchemaValidator.getInstance(
            "/standards/ebl/schemas/EBL_v3.0.0.yaml", "TransportDocumentAmend");
    var td =
        (ObjectNode)
            new CarrierSupplyPayloadAction(
                    "Carrier", ScenarioType.REGULAR_SWB, "3.0.0", null, true, true)
                .getJsonForHumanReadablePrompt();
    td.put("transportDocumentReference", "TDR");
    var amendment = OBJECT_MAPPER.createObjectNode();
    amendment.put("amendedTransportDocumentStatus", "AMENDMENT_RECEIVED");
    amendment.set("amendedTransportDocument", td);
    assertTrue(schema.validate(amendment).isEmpty());
    for (boolean carrierScenario : List.of(false, true)) {
      for (int status : List.of(200, 201, 202, 204, 206, 299)) {
        for (String body : List.of("", "{}", " ", "null", amendment.toString())) {
          var previous =
              new UC8_Carrier_IssueTransportDocumentAction(
                  "Carrier", "Shipper", null, null, false, ScenarioType.REGULAR_SWB);
          var input = OBJECT_MAPPER.createObjectNode();
          input.putObject("input").put("transportDocumentReference", "TDR");
          previous.doHandlePartyInput(input);
          var action =
              new Shipper_GetTransportDocumentAmendmentAction(
                  "Carrier",
                  "Shipper",
                  previous,
                  schema,
                  AmendedTransportDocumentStatus.AMENDMENT_RECEIVED,
                  carrierScenario);
          var headers = Map.of("API-Version", List.of("3.0.0"));
          var request =
              new ConformanceRequest(
                  "GET",
                  "http://localhost/v3/transport-documents/TDR/amendment",
                  Map.of(),
                  new ConformanceMessage(
                      "Shipper",
                      "Shipper",
                      "Carrier",
                      "Carrier",
                      Map.of("API-Version", List.of("3")),
                      new ConformanceMessageBody(""),
                      0));
          var originalExchange =
              new ConformanceExchange(
                  request,
                  request.createResponse(status, headers, new ConformanceMessageBody(body)));
          var exchange =
              ConformanceExchange.fromJson(
                  (ObjectNode) OBJECT_MAPPER.readTree(originalExchange.toJson().toString()));
          action.handleExchange(exchange);
          var check = action.createCheck("3.0.0");
          check.check(id -> exchange);
          var results = results(check).toList();
          assertFalse(results.isEmpty());
          boolean expected =
              (status == 200 || carrierScenario && status != 202)
                      && body.equals(amendment.toString())
                  || carrierScenario && status == 202 && body.isEmpty();
          assertEquals(
              expected,
              results.stream().allMatch(ConformanceResult::isConformant),
              "carrier="
                  + carrierScenario
                  + ", status="
                  + status
                  + ", body length="
                  + body.length()
                  + ", errors="
                  + results.stream().flatMap(r -> r.getErrors().stream()).toList());
        }
      }
    }
  }

  private static Stream<ConformanceResult> results(ConformanceCheck check) {
    return Stream.concat(
        check.resultsStream(),
        check.subChecksStream().flatMap(Shipper_GetTransportDocumentAmendmentActionTest::results));
  }
}
