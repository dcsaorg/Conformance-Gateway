package org.dcsa.conformance.standards.ovs.checks;

import static org.dcsa.conformance.standards.ovs.checks.OvsChecks.VALID_DEPRECATED_STATUS_CODE;
import static org.dcsa.conformance.standards.ovs.checks.OvsChecks.VALID_STATUS_CODES;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

  @Nested
  class CheckServiceSchedulesExist {

    @Test
    void validSchedules_allHaveVesselSchedules() {
      schedule.put("carrierServiceName", "ServiceA");
      schedule.putArray("vesselSchedules").addObject().put("vesselIMONumber", "12345");
      assertTrue(OvsChecks.checkServiceSchedulesExist(body).isEmpty());
    }

    @Test
    void nullBody() {
      assertFalse(OvsChecks.checkServiceSchedulesExist(null).isEmpty());
    }

    @Test
    void emptyArrayBody() {
      assertTrue(OvsChecks.checkServiceSchedulesExist(mapper.createArrayNode()).isEmpty());
    }

    @Test
    void missingVesselSchedules() {
      schedule.put("carrierServiceName", "ServiceA");
      assertFalse(OvsChecks.checkServiceSchedulesExist(body).isEmpty());
    }

    @Test
    void emptyVesselSchedulesArray() {
      schedule.put("carrierServiceName", "ServiceA");
      schedule.putArray("vesselSchedules");
      assertFalse(OvsChecks.checkServiceSchedulesExist(body).isEmpty());
    }
  }

  @Nested
  class ValidStatusCodes {

    @Test
    void invalidCode_returnsError() {
      addTransportCallWithStatusCodes("INVALID");
      assertFalse(VALID_STATUS_CODES.validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void validCode_noError() {
      addTransportCallWithStatusCodes("OMIT");
      assertTrue(VALID_STATUS_CODES.validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void allValidCodes_noError() {
      addTransportCallWithStatusCodes("OMIT", "BLNK", "ADHO");
      assertTrue(VALID_STATUS_CODES.validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void extendedCodes_validForStatusCodesOnly_noError() {
      addTransportCallWithStatusCodes("DRYD", "BUNK", "OOSV");
      assertTrue(VALID_STATUS_CODES.validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void oneInvalidAmongMultiple_returnsError() {
      addTransportCallWithStatusCodes("OMIT", "CU", "BLNK");
      assertFalse(VALID_STATUS_CODES.validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void absent_isIrrelevant() {
      addTransportCallWithoutStatusCodes();
      assertFalse(VALID_STATUS_CODES.validate(body).isRelevant());
    }

    @Test
    void emptyArray_isIrrelevant() {
      getTransportCallNode().putArray("statusCodes");
      assertFalse(VALID_STATUS_CODES.validate(body).isRelevant());
    }

    @Test
    void noTransportCalls_isIrrelevant() {
      schedule.putArray("vesselSchedules").addObject();
      assertFalse(VALID_STATUS_CODES.validate(body).isRelevant());
    }
  }

  @Nested
  class ValidDeprecatedStatusCode {

    @Test
    void invalidCode_returnsError() {
      addTransportCallWithDeprecatedStatusCode("ARRIVED");
      assertFalse(VALID_DEPRECATED_STATUS_CODE.validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void validCode_noError() {
      addTransportCallWithDeprecatedStatusCode("BLNK");
      assertTrue(VALID_DEPRECATED_STATUS_CODE.validate(body).getErrorMessages().isEmpty());
    }

    @Test
    void absent_isIrrelevant() {
      addTransportCallWithoutStatusCodes();
      assertFalse(VALID_DEPRECATED_STATUS_CODE.validate(body).isRelevant());
    }

    @Test
    void ignoredWhenStatusCodesPresent_isIrrelevant() {
      ObjectNode transportCall = getTransportCallNode();
      transportCall.putArray("statusCodes").add("OMIT");
      transportCall.put("statusCode", "ARRIVED");
      assertFalse(VALID_DEPRECATED_STATUS_CODE.validate(body).isRelevant());
    }

    @Test
    void ignoredWhenStatusCodesEmptyArrayPresent_isIrrelevant() {
      ObjectNode transportCall = getTransportCallNode();
      transportCall.putArray("statusCodes"); // empty array
      transportCall.put("statusCode", "ARRIVED");
      assertFalse(VALID_DEPRECATED_STATUS_CODE.validate(body).isRelevant());
    }
  }

  // --- Helpers ---

  private ObjectNode getTransportCallNode() {
    schedule.putArray("vesselSchedules")
        .addObject()
        .putArray("transportCalls")
        .addObject();
    return (ObjectNode) schedule.path("vesselSchedules").get(0)
        .path("transportCalls").get(0);
  }

  private void addTransportCallWithStatusCodes(String... codes) {
    ArrayNode statusCodes = getTransportCallNode().putArray("statusCodes");
    for (String code : codes) {
      statusCodes.add(code);
    }
  }

  private void addTransportCallWithDeprecatedStatusCode(String code) {
    getTransportCallNode().put("statusCode", code);
  }

  private void addTransportCallWithoutStatusCodes() {
    getTransportCallNode().put("transportCallReference", "REF001");
  }
}
