package org.dcsa.conformance.standards.booking.party;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.junit.jupiter.api.Test;

class DynamicScenarioParametersTest {

  private final DynamicScenarioParameters dsp =
      new DynamicScenarioParameters(ScenarioType.DG, "cbrr", "cbr", null, null);

  private final CarrierScenarioParameters csp =
      new CarrierScenarioParameters(
          "scr", "cqr", "cevn", null, "h1", "ct1", "h2", "ct2", "pol", "pod");

  @Test
  void testDSPToJsonConversion() {
    JsonNode jsonNode = dsp.toJson();
    assertEquals(3, (long) jsonNode.properties().size());
  }

  @Test
  void testDSPFromJsonNode() {
    DynamicScenarioParameters dspNew = DynamicScenarioParameters.fromJson(dsp.toJson());
    assertEquals(dsp.toString(), dspNew.toString());
  }

  @Test
  void testCSPToJsonConversion() {
    JsonNode jsonNode = csp.toJson();
    assertEquals(10, (long) jsonNode.properties().size());
  }

  @Test
  void testCSPFromJsonNode() {
    CarrierScenarioParameters cspNew = CarrierScenarioParameters.fromJson(csp.toJson());
    assertEquals(csp.toString(), cspNew.toString());
  }
}
