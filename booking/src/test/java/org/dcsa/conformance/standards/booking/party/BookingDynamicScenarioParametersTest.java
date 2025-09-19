package org.dcsa.conformance.standards.booking.party;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.standardscommons.party.BookingDynamicScenarioParameters;
import org.junit.jupiter.api.Test;

class BookingDynamicScenarioParametersTest {

  private final BookingDynamicScenarioParameters dsp =
      new BookingDynamicScenarioParameters(ScenarioType.DG.name(), "12345", "54321");

  @Test
  void testDSPToJsonConversion() {
    JsonNode jsonNode = dsp.toJson();
    assertEquals(3, (long) jsonNode.properties().size());
  }

  @Test
  void testDSPFromJsonNode() {
    BookingDynamicScenarioParameters dspNew =
        BookingDynamicScenarioParameters.fromJson(dsp.toJson());
    assertEquals(dsp.toString(), dspNew.toString());
  }
}
