package org.dcsa.conformance.standards.booking.party;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class EblDynamicScenarioParametersTest {

  private final EblDynamicScenarioParameters dsp =
      new EblDynamicScenarioParameters(
          null,
          null,
          null,
          null,
          false);

  @Test
  void testDSPToJsonConversion() {
    JsonNode jsonNode = dsp.toJson();
    assertEquals(3, (long) jsonNode.properties().size());
  }

  @Test
  void testDSPFromJsonNode() {
    EblDynamicScenarioParameters dspNew = EblDynamicScenarioParameters.fromJson(dsp.toJson());
    assertEquals(dsp.toString(), dspNew.toString());
  }
}
