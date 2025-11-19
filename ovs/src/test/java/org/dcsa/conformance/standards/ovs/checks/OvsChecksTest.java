package org.dcsa.conformance.standards.ovs.checks;

import static org.dcsa.conformance.standards.ovs.checks.OvsChecks.VALID_STATUS_CODE;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OvsChecksTest {

  private ObjectMapper mapper;
  private ArrayNode body;
  private ObjectNode schedule;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    body = mapper.createArrayNode();
    schedule = body.addObject();
  }

  @Test
  void testValidSchedules_allHaveVesselSchedules() {

    schedule.put("carrierServiceName", "ServiceA");
    ArrayNode vesselSchedules = schedule.putArray("vesselSchedules");
    ObjectNode vesselSchedule = vesselSchedules.addObject();
    vesselSchedule.put("vesselIMONumber", "12345");

    Set<String> errors = OvsChecks.checkServiceSchedulesExist(body);

    assertTrue(errors.isEmpty());
  }

  @Test
  void testNullBody() {
    Set<String> errors = OvsChecks.checkServiceSchedulesExist(null);
    assertFalse(errors.isEmpty());
  }

  @Test
  void testSomeMissingVesselSchedules() {
    schedule.put("carrierServiceName", "ServiceA");
    Set<String> errors = OvsChecks.checkServiceSchedulesExist(body);

    assertFalse(errors.isEmpty());
  }

  @Test
  void testInvalidValidStatusCode() {
    schedule.put("carrierServiceName", "ServiceA");
    ArrayNode vesselSchedules = schedule.putArray("vesselSchedules");
    ObjectNode vesselSchedule = vesselSchedules.addObject();
    vesselSchedule.put("vesselIMONumber", "12345");
    vesselSchedule.putArray("transportCalls").addObject().put("statusCode", "ARRIVED");
    Set<String> errors = VALID_STATUS_CODE.validate(body).getErrorMessages();

    assertFalse(errors.isEmpty());
  }

  @Test
  void testValidStatusCode() {
    schedule.put("carrierServiceName", "ServiceA");
    ArrayNode vesselSchedules = schedule.putArray("vesselSchedules");
    ObjectNode vesselSchedule = vesselSchedules.addObject();
    vesselSchedule.put("vesselIMONumber", "12345");
    vesselSchedule.putArray("transportCalls").addObject().put("statusCode", "BLNK");
    Set<String> errors = VALID_STATUS_CODE.validate(body).getErrorMessages();

    assertTrue(errors.isEmpty());
  }
}
