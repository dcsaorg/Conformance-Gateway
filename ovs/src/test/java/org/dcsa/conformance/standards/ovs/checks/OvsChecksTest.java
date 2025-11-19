package org.dcsa.conformance.standards.ovs.checks;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;

class OvsChecksTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private JsonNode obj(Object data) {
    return mapper.valueToTree(data);
  }

  @Test
  void testValidSchedules_allHaveVesselSchedules() {
    List<Map<String, Object>> schedules =
        List.of(
            Map.of(
                "carrierServiceName",
                "ServiceA",
                "vesselSchedules",
                List.of(Map.of("vesselIMONumber", "12345"))),
            Map.of(
                "carrierServiceName",
                "ServiceB",
                "vesselSchedules",
                List.of(Map.of("vesselIMONumber", "67890"))));

    JsonNode body = obj(schedules);

    Set<String> errors = OvsChecks.checkServiceSchedulesExist(body);

    assertTrue(errors.isEmpty());
  }

  @Test
  void testNullBody() {
    Set<String> errors = OvsChecks.checkServiceSchedulesExist(null);
    assertFalse(errors.isEmpty());
  }

  @Test
  void testNonArrayBody() {
    Map<String, Object> nonArray = Map.of("vesselSchedules", List.of());
    JsonNode body = obj(nonArray);

    Set<String> errors = OvsChecks.checkServiceSchedulesExist(body);

    assertFalse(errors.isEmpty());
  }

  @Test
  void testEmptyArray() {
    JsonNode body = obj(List.of());

    Set<String> errors = OvsChecks.checkServiceSchedulesExist(body);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testSomeMissingVesselSchedules() {
    List<Map<String, Object>> schedules =
        List.of(
            Map.of(
                "carrierServiceName",
                "ServiceA",
                "vesselSchedules",
                List.of(Map.of("vesselIMONumber", "12345"))),
            Map.of("carrierServiceName", "ServiceB"));

    JsonNode body = obj(schedules);

    Set<String> errors = OvsChecks.checkServiceSchedulesExist(body);

    assertFalse(errors.isEmpty());
  }

  @Test
  void testAllMissingVesselSchedules() {
    List<Map<String, Object>> schedules =
        List.of(Map.of("carrierServiceName", "ServiceA"), Map.of("carrierServiceName", "ServiceB"));

    JsonNode body = obj(schedules);

    Set<String> errors = OvsChecks.checkServiceSchedulesExist(body);

    assertFalse(errors.isEmpty());
  }

  @Test
  void testEmptyVesselSchedulesArray() {
    List<Map<String, Object>> schedules =
        List.of(
            Map.of(
                "carrierServiceName", "ServiceA", "vesselSchedules", List.of() // empty -> invalid
                ));

    JsonNode body = obj(schedules);

    Set<String> errors = OvsChecks.checkServiceSchedulesExist(body);
    assertFalse(errors.isEmpty());
  }


}
