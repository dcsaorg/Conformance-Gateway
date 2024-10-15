package org.dcsa.conformance.standards.booking.party;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.junit.jupiter.api.Test;

class DynamicScenarioParametersTest {

  private final DynamicScenarioParameters dsp =
      new DynamicScenarioParameters(ScenarioType.DG, "cbrr", "cbr", null, null);

  @Test
  void testToJsonConversion() {
    JsonNode jsonNode = dsp.toJson();
    assertEquals(3, (long) jsonNode.properties().size());
  }

  @Test
  void testFromJsonNode() {
    DynamicScenarioParameters dspNew = DynamicScenarioParameters.fromJson(dsp.toJson());
    assertEquals(dsp.toString(), dspNew.toString());
  }
}
