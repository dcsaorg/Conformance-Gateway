package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.standardscommons.party.EblDynamicScenarioParameters;

final class EblCheckTestFixtures {
  private EblCheckTestFixtures() {}

  static EblDynamicScenarioParameters dsp(String scenarioType) {
    return new EblDynamicScenarioParameters(
        scenarioType, "SIR", "TDR", "DRAFT", null, null, false, false);
  }

  static ConformanceExchange exchange(JsonNode requestBody, JsonNode responseBody) {
    var request =
        new ConformanceRequest(
            "POST",
            "http://localhost/v3/test",
            Map.of(),
            new ConformanceMessage(
                "Carrier",
                "Carrier",
                "Shipper",
                "Shipper",
                Map.of(),
                new ConformanceMessageBody(requestBody),
                0));
    return new ConformanceExchange(
        request, request.createResponse(200, Map.of(), new ConformanceMessageBody(responseBody)));
  }
}
