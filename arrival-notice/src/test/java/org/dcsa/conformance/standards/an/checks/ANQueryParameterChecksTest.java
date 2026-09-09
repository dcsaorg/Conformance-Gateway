package org.dcsa.conformance.standards.an.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ANQueryParameterChecksTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String FIXTURE =
    "/standards/an/messages/arrivalnotice-api-100-get-basic-response.json";

  @Test
  void matchesEveryDocumentedFilterAndIgnoresLimit() throws IOException {
    Map<String, String> parameters = new LinkedHashMap<>();
    parameters.put("transportDocumentReferences", "OTHER,HHL71800000");
    parameters.put("equipmentReferences", "OTHER,HLCU1234567");
    parameters.put("portOfDischarge", "SGSIN");
    parameters.put("vesselIMONumber", "9321483");
    parameters.put("vesselName", "YM MASCULINITY");
    parameters.put("carrierImportVoyageNumber", "097E");
    parameters.put("universalImportVoyageReference", "2301W");
    parameters.put("carrierServiceCode", "FE1");
    parameters.put("universalServiceReference", "SR12345A");
    parameters.put("portOfDischargeArrivalDateMin", "2024-03-01");
    parameters.put("portOfDischargeArrivalDateMax", "2024-03-31");
    parameters.put("limit", "1");

    assertTrue(ANQueryParameterChecks.matchesAll(firstArrivalNotice(), parameters));
  }

  @Test
  void rejectsMismatchesAndInvalidDates() throws IOException {
    JsonNode notice = firstArrivalNotice();
    assertFalse(
      ANQueryParameterChecks.matchesAll(
        notice, Map.of("transportDocumentReferences", "OTHER")));
    assertFalse(
      ANQueryParameterChecks.matchesAll(notice, Map.of("equipmentReferences", "OTHER")));
    assertFalse(ANQueryParameterChecks.matchesAll(notice, Map.of("portOfDischarge", "NLRTM")));
    assertFalse(ANQueryParameterChecks.matchesAll(notice, Map.of("vesselName", "Other Vessel")));
    assertFalse(
      ANQueryParameterChecks.matchesAll(
        notice, Map.of("portOfDischargeArrivalDateMin", "2024-04-01")));
    assertFalse(
      ANQueryParameterChecks.matchesAll(
        notice, Map.of("portOfDischargeArrivalDateMax", "2024-03-01")));
    assertFalse(
      ANQueryParameterChecks.matchesAll(
        notice, Map.of("portOfDischargeArrivalDateMin", "not-a-date")));
    assertFalse(
      ANQueryParameterChecks.matchesAll(
        notice, Map.of("unsupportedFilter", "value")));
  }

  private static JsonNode firstArrivalNotice() throws IOException {
    try (var stream = ANQueryParameterChecksTest.class.getResourceAsStream(FIXTURE)) {
      JsonNode notice = MAPPER.readTree(stream).path("arrivalNotices").path(0);
      ((com.fasterxml.jackson.databind.node.ObjectNode) notice)
        .put("transportDocumentReference", "HHL71800000");
      return notice;
    }
  }
}


