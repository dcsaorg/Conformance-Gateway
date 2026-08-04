package org.dcsa.conformance.standards.ovs.checks;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OvsChecksTest {

  private ObjectMapper mapper;
  private List<org.dcsa.conformance.core.check.JsonContentCheck> checks;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    checks = OvsChecks.buildResponseContentChecks();
  }

  @Nested
  class MandatoryValidations {

    @Test
    void serviceSchedulesMustExist() {
      assertFalse(checks.get(0).validate(null).getErrorMessages().isEmpty());
      assertTrue(checks.get(0).validate(validResponse()).getErrorMessages().isEmpty());
    }

    @Test
    void vesselSchedulesMustExist() {
      ArrayNode body = mapper.createArrayNode();
      body.addObject().put("carrierServiceCode", "BW1");

      assertFalse(checks.get(1).validate(body).getErrorMessages().isEmpty());
      assertTrue(checks.get(1).validate(validResponse()).getErrorMessages().isEmpty());
    }

    @Test
    void transportCallsMustExist() {
      ArrayNode body = mapper.createArrayNode();
      body.addObject().putArray("vesselSchedules").addObject();

      assertFalse(checks.get(2).validate(body).getErrorMessages().isEmpty());
      assertTrue(checks.get(2).validate(validResponse()).getErrorMessages().isEmpty());
    }

    @Test
    void locationMustExist() {
      ArrayNode body = responseWithTransportCall();
      assertFalse(checks.get(3).validate(body).getErrorMessages().isEmpty());
      assertTrue(checks.get(3).validate(validResponse()).getErrorMessages().isEmpty());
    }

    @Test
    void timestampsMustExist() {
      ArrayNode body = responseWithTransportCall();
      assertFalse(checks.get(4).validate(body).getErrorMessages().isEmpty());
      assertTrue(checks.get(4).validate(validResponse()).getErrorMessages().isEmpty());
    }
  }

  @Nested
  class OptionalValidations {

    @Test
    void universalServiceReference_isIrrelevantWhenAbsent() {
      assertFalse(checks.get(5).validate(validResponse()).isRelevant());
    }

    @Test
    void universalServiceReference_failsWhenOnlyBlankValuesPresent() {
      ArrayNode body = validResponse();
      ((ObjectNode) body.get(0)).put("universalServiceReference", " ");
      assertFalse(checks.get(5).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void universalServiceReference_passesWhenNonBlankValuePresent() {
      ArrayNode body = validResponse();
      ((ObjectNode) body.get(0)).put("universalServiceReference", "SR12345A");
      assertTrue(checks.get(5).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void universalVoyageReference_isIrrelevantWhenAbsent() {
      assertFalse(checks.get(6).validate(validResponse()).isRelevant());
    }

    @Test
    void universalVoyageReference_failsWhenOnlyBlankValuesPresent() {
      ArrayNode body = validResponse();
      ObjectNode transportCall = firstTransportCall(body);
      transportCall.put("universalImportVoyageReference", " ");
      transportCall.put("universalExportVoyageReference", " ");
      assertFalse(checks.get(6).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void universalVoyageReference_passesWhenOneNonBlankValuePresent() {
      ArrayNode body = validResponse();
      firstTransportCall(body).put("universalExportVoyageReference", "2103N");
      assertTrue(checks.get(6).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void statusCodes_isIrrelevantWhenAbsent() {
      assertFalse(checks.get(7).validate(validResponse()).isRelevant());
    }

    @Test
    void statusCodes_failsWhenPresentButEmpty() {
      ArrayNode body = validResponse();
      firstTransportCall(body).putArray("statusCodes");
      assertFalse(checks.get(7).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void statusCodes_passesWhenArrayHasValues() {
      ArrayNode body = validResponse();
      firstTransportCall(body).putArray("statusCodes").add("OMIT");
      assertTrue(checks.get(7).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void dummyVesselName_isIrrelevantWhenNoDummyVesselExists() {
      assertFalse(checks.get(8).validate(validResponse()).isRelevant());
    }

    @Test
    void dummyVesselName_failsWhenDummyVesselHasBlankName() {
      ArrayNode body = validResponse();
      ObjectNode vesselSchedule = (ObjectNode) body.get(0).path("vesselSchedules").get(0);
      vesselSchedule.put("isDummyVessel", true);
      vesselSchedule.put("vesselName", "");
      assertFalse(checks.get(8).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void dummyVesselName_passesWhenDummyVesselHasName() {
      ArrayNode body = validResponse();
      ObjectNode vesselSchedule = (ObjectNode) body.get(0).path("vesselSchedules").get(0);
      vesselSchedule.put("isDummyVessel", true);
      vesselSchedule.put("vesselName", "DUMMY VESSEL");
      assertTrue(checks.get(8).validate(body).getErrorMessages().isEmpty());
    }
  }

  private ArrayNode validResponse() {
    ArrayNode body = mapper.createArrayNode();
    ObjectNode schedule = body.addObject();
    ObjectNode vesselSchedule = schedule.putArray("vesselSchedules").addObject();
    ObjectNode transportCall = vesselSchedule.putArray("transportCalls").addObject();
    transportCall.putObject("location").put("UNLocationCode", "NLAMS");
    transportCall.putArray("timestamps").addObject().put("eventDateTime", "2025-01-01T00:00:00Z");
    return body;
  }

  private ArrayNode responseWithTransportCall() {
    ArrayNode body = mapper.createArrayNode();
    ObjectNode schedule = body.addObject();
    schedule.putArray("vesselSchedules").addObject().putArray("transportCalls").addObject();
    return body;
  }

  private ObjectNode firstTransportCall(ArrayNode body) {
    return (ObjectNode)
        body.get(0).path("vesselSchedules").get(0).path("transportCalls").get(0);
  }
}
