package org.dcsa.conformance.standards.ovs.checks;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.UUID;

import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.report.ConformanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OvsChecksTest {

  private ObjectMapper mapper;
  private List<JsonContentCheck> checks;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    checks = OvsChecks.buildResponseContentChecks();
  }

  @Nested
  class MandatoryValidations {

    @Test
    void shouldFailWhenResponseBodyIsNull() {
      assertFalse(checks.get(0).validate(null).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailWhenResponseBodyIsNotArray() {
      ObjectNode body = mapper.createObjectNode();
      assertFalse(checks.get(0).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailWhenResponseBodyIsJsonNullNode() {
      assertFalse(checks.get(0).validate(NullNode.getInstance()).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailWhenResponseBodyIsMissingNode() {
      assertFalse(checks.get(0).validate(MissingNode.getInstance()).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailWhenResponseBodyIsEmptyArray() {
      ArrayNode body = mapper.createArrayNode();
      assertFalse(checks.get(0).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenAtLeastOneServiceScheduleExists() {
      assertTrue(checks.get(0).validate(validResponse()).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailWhenNoScheduleContainsVesselSchedules() {
      ArrayNode body = mapper.createArrayNode();
      body.addObject().put("carrierServiceCode", "BW1");

      assertFalse(checks.get(1).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenAtLeastOneScheduleContainsVesselSchedules() {
      assertTrue(checks.get(1).validate(validResponse()).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailWhenNoVesselScheduleContainsTransportCalls() {
      ArrayNode body = mapper.createArrayNode();
      body.addObject().putArray("vesselSchedules").addObject();

      assertFalse(checks.get(2).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenAtLeastOneVesselScheduleContainsTransportCalls() {
      assertTrue(checks.get(2).validate(validResponse()).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailWhenNoTransportCallContainsLocationObject() {
      ArrayNode body = responseWithTransportCall();
      assertFalse(checks.get(3).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenAtLeastOneTransportCallContainsLocationObject() {
      assertTrue(checks.get(3).validate(validResponse()).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailWhenNoTransportCallContainsTimestampsArray() {
      ArrayNode body = responseWithTransportCall();
      assertFalse(checks.get(4).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenAtLeastOneTransportCallContainsTimestampsArray() {
      assertTrue(checks.get(4).validate(validResponse()).getErrorMessages().isEmpty());
    }
  }

  @Nested
  class OptionalValidations {

    @Test
    void shouldBeIrrelevantWhenUniversalServiceReferenceIsAbsent() {
      assertFalse(checks.get(5).validate(validResponse()).isRelevant());
    }

    @Test
    void shouldFailWhenUniversalServiceReferenceIsBlank() {
      ArrayNode body = validResponse();
      ((ObjectNode) body.get(0)).put("universalServiceReference", " ");
      assertFalse(checks.get(5).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenUniversalServiceReferenceIsNonBlank() {
      ArrayNode body = validResponse();
      ((ObjectNode) body.get(0)).put("universalServiceReference", "SR12345A");
      assertTrue(checks.get(5).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenUniversalServiceReferenceIsValidInSecondSchedule() {
      ArrayNode body = validResponseWithTwoSchedules();
      ((ObjectNode) body.get(1)).put("universalServiceReference", "SR67890B");
      assertTrue(checks.get(5).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldBeIrrelevantWhenUniversalVoyageReferencesAreAbsent() {
      assertFalse(checks.get(6).validate(validResponse()).isRelevant());
    }

    @Test
    void shouldFailWhenUniversalVoyageReferencesAreBlank() {
      ArrayNode body = validResponse();
      ObjectNode transportCall = firstTransportCall(body);
      transportCall.put("universalImportVoyageReference", " ");
      transportCall.put("universalExportVoyageReference", " ");
      assertFalse(checks.get(6).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenUniversalExportVoyageReferenceIsNonBlank() {
      ArrayNode body = validResponse();
      firstTransportCall(body).put("universalExportVoyageReference", "2103N");
      assertTrue(checks.get(6).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenUniversalImportVoyageReferenceIsNonBlank() {
      ArrayNode body = validResponse();
      firstTransportCall(body).put("universalImportVoyageReference", "2104S");
      assertTrue(checks.get(6).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldBeIrrelevantWhenStatusCodesIsAbsent() {
      assertFalse(checks.get(7).validate(validResponse()).isRelevant());
    }

    @Test
    void shouldFailWhenStatusCodesIsEmptyArray() {
      ArrayNode body = validResponse();
      firstTransportCall(body).putArray("statusCodes");
      assertFalse(checks.get(7).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailWhenStatusCodesIsNotArray() {
      ArrayNode body = validResponse();
      firstTransportCall(body).put("statusCodes", "OMIT");
      assertFalse(checks.get(7).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenStatusCodesContainsAtLeastOneValue() {
      ArrayNode body = validResponse();
      firstTransportCall(body).putArray("statusCodes").add("OMIT");
      assertTrue(checks.get(7).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenStatusCodesIsValidInSecondTransportCall() {
      ArrayNode body = responseWithTwoTransportCalls();
      secondTransportCall(body).putArray("statusCodes").add("OMIT");
      assertTrue(checks.get(7).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldBeIrrelevantWhenNoDummyVesselExists() {
      assertFalse(checks.get(8).validate(validResponse()).isRelevant());
    }

    @Test
    void shouldFailWhenDummyVesselNameIsBlank() {
      ArrayNode body = validResponse();
      ObjectNode vesselSchedule = (ObjectNode) body.get(0).path("vesselSchedules").get(0);
      vesselSchedule.put("isDummyVessel", true);
      vesselSchedule.put("vesselName", "");
      assertFalse(checks.get(8).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldPassWhenDummyVesselNameIsNonBlank() {
      ArrayNode body = validResponse();
      ObjectNode vesselSchedule = (ObjectNode) body.get(0).path("vesselSchedules").get(0);
      vesselSchedule.put("isDummyVessel", true);
      vesselSchedule.put("vesselName", "DUMMY VESSEL");
      assertTrue(checks.get(8).validate(body).getErrorMessages().isEmpty());
    }
  }

  @Nested
  class ActionCheckBuilders {

    @Test
    void shouldCreateMandatoryActionCheckWithoutStatusOverride() {
      ActionCheck actionCheck = OvsChecks.mandatoryResponseContentChecks(UUID.randomUUID(), "3.0.0");
      assertNotNull(actionCheck);
      assertNull(actionCheck.getStatusOverride());
    }

    @Test
    void shouldCreateOptionalActionCheckWithPartialConformanceOverride() {
      ActionCheck actionCheck = OvsChecks.optionalResponseContentChecks(UUID.randomUUID(), "3.0.0");
      assertNotNull(actionCheck);
      assertEquals(ConformanceStatus.PARTIALLY_CONFORMANT, actionCheck.getStatusOverride());
      assertFalse(actionCheck.isStatusOverridePropagated());
    }
  }

  @Nested
  class BranchCoverage {

    @Test
    void shouldFailVesselSchedulesValidationWhenBodyIsNull() {
      assertFalse(checks.get(1).validate(null).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailTransportCallsValidationWhenBodyIsNull() {
      assertFalse(checks.get(2).validate(null).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailLocationValidationWhenBodyIsNull() {
      assertFalse(checks.get(3).validate(null).getErrorMessages().isEmpty());
    }

    @Test
    void shouldFailTimestampsValidationWhenBodyIsNull() {
      assertFalse(checks.get(4).validate(null).getErrorMessages().isEmpty());
    }

    @Test
    void shouldMarkUniversalServiceReferenceAsIrrelevantWhenBodyIsNull() {
      assertFalse(checks.get(5).validate(null).isRelevant());
    }

    @Test
    void shouldMarkUniversalVoyageReferenceAsIrrelevantWhenBodyIsNull() {
      assertFalse(checks.get(6).validate(null).isRelevant());
    }

    @Test
    void shouldMarkStatusCodesAsIrrelevantWhenBodyIsNull() {
      assertFalse(checks.get(7).validate(null).isRelevant());
    }

    @Test
    void shouldMarkDummyVesselNameAsIrrelevantWhenBodyIsNull() {
      assertFalse(checks.get(8).validate(null).isRelevant());
    }

    @Test
    void shouldContinueWhenScheduleHasNoVesselSchedulesInTransportCallsValidation() {
      ArrayNode body = mapper.createArrayNode();
      body.addObject();
      ObjectNode secondSchedule = body.addObject();
      secondSchedule.putArray("vesselSchedules").addObject();
      assertFalse(checks.get(2).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldContinueWhenScheduleHasNoVesselSchedulesInLocationValidation() {
      ArrayNode body = mapper.createArrayNode();
      body.addObject();
      ObjectNode secondSchedule = body.addObject();
      ObjectNode vesselSchedule = secondSchedule.putArray("vesselSchedules").addObject();
      vesselSchedule.putArray("transportCalls").addObject();
      assertFalse(checks.get(3).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldContinueWhenVesselScheduleHasNoTransportCallsInLocationValidation() {
      ArrayNode body = mapper.createArrayNode();
      ObjectNode schedule = body.addObject();
      ArrayNode vesselSchedules = schedule.putArray("vesselSchedules");
      vesselSchedules.addObject();
      vesselSchedules.addObject().putArray("transportCalls").addObject();
      assertFalse(checks.get(3).validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void shouldContinueWhenVesselScheduleHasNoTransportCallsInStatusCodesValidation() {
      ArrayNode body = mapper.createArrayNode();
      ObjectNode schedule = body.addObject();
      ArrayNode vesselSchedules = schedule.putArray("vesselSchedules");
      vesselSchedules.addObject();
      vesselSchedules.addObject().putArray("transportCalls").addObject();
      assertFalse(checks.get(7).validate(body).isRelevant());
    }

    @Test
    void shouldContinueWhenScheduleHasNoVesselSchedulesInStatusCodesValidation() {
      ArrayNode body = mapper.createArrayNode();
      body.addObject();
      ObjectNode secondSchedule = body.addObject();
      secondSchedule.putArray("vesselSchedules").addObject().putArray("transportCalls").addObject();
      assertFalse(checks.get(7).validate(body).isRelevant());
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

  private ArrayNode validResponseWithTwoSchedules() {
    ArrayNode body = validResponse();
    ObjectNode schedule = body.addObject();
    ObjectNode vesselSchedule = schedule.putArray("vesselSchedules").addObject();
    ObjectNode transportCall = vesselSchedule.putArray("transportCalls").addObject();
    transportCall.putObject("location").put("UNLocationCode", "USNYC");
    transportCall.putArray("timestamps").addObject().put("eventDateTime", "2025-01-02T00:00:00Z");
    return body;
  }

  private ArrayNode responseWithTwoTransportCalls() {
    ArrayNode body = validResponse();
    ArrayNode transportCalls =
        (ArrayNode) body.get(0).path("vesselSchedules").get(0).path("transportCalls");
    transportCalls.addObject();
    return body;
  }

  private ObjectNode firstTransportCall(ArrayNode body) {
    return (ObjectNode)
        body.get(0).path("vesselSchedules").get(0).path("transportCalls").get(0);
  }

  private ObjectNode secondTransportCall(ArrayNode body) {
    return (ObjectNode)
        body.get(0).path("vesselSchedules").get(0).path("transportCalls").get(1);
  }
}
