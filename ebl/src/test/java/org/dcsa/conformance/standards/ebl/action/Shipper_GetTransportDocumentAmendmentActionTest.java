package org.dcsa.conformance.standards.ebl.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.dcsa.conformance.core.check.ConformanceResult;
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
}
