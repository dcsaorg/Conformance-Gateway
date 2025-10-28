package org.dcsa.conformance.standards.cs.checks;

import static org.dcsa.conformance.standards.cs.checks.CsChecks.VALIDATE_CUTOFF_TIME_CODE;
import static org.dcsa.conformance.standards.cs.checks.CsChecks.VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP;
import static org.dcsa.conformance.standards.cs.checks.CsChecks.VALIDATE_CUTOFF_TIME_CODE_PS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class CsChecksTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ArrayNode rootNodeArray = objectMapper.createArrayNode();
  private final ObjectNode routing = objectMapper.createObjectNode();

  @Test
  void testValidateCutoffTimeCodePtp() {
    ArrayNode cutOffTimes = routing.putArray("cutOffTimes");
    ObjectNode cutOffTime = cutOffTimes.addObject();
    cutOffTime.put("cutOffDateTimeCode", "LCO");
    rootNodeArray.add(routing);
    assertTrue(VALIDATE_CUTOFF_TIME_CODE.validate(rootNodeArray).getErrorMessages().isEmpty());

    cutOffTime.put("cutOffDateTimeCode", "LCR");
    rootNodeArray.add(routing);
    assertFalse(VALIDATE_CUTOFF_TIME_CODE.validate(rootNodeArray).getErrorMessages().isEmpty());
  }

  @Test
  void testValidateCutoffTimeCodeAndReceiptTypeAtOriginPtp() {
    ArrayNode cutOffTimes = routing.putArray("cutOffTimes");
    ObjectNode cutOffTime = cutOffTimes.addObject();
    cutOffTime.put("cutOffDateTimeCode", "LCO");
    routing.put("receiptTypeAtOrigin", "CFS");
    rootNodeArray.add(routing);
    assertTrue(VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP.validate(rootNodeArray).getErrorMessages().isEmpty());

    cutOffTime.put("cutOffDateTimeCode", "LCO");
    routing.put("receiptTypeAtOrigin", "CO");
    assertFalse(VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP.validate(rootNodeArray).getErrorMessages().isEmpty());

    cutOffTime.put("cutOffDateTimeCode", "PCO");
    routing.put("receiptTypeAtOrigin", "CO");
    assertTrue(VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP.validate(rootNodeArray).getErrorMessages().isEmpty());
  }

  @Test
  void testValidateCutoffTimeCodePs() {
    ObjectNode schedule =  objectMapper.createObjectNode();
    ArrayNode vesselSchedules = schedule.putArray("vesselSchedules");
    ObjectNode vesselSchedule = vesselSchedules.addObject();
    ArrayNode cutOffTimes = vesselSchedule.putArray("cutOffTimes");
    ObjectNode cutOffTime = cutOffTimes.addObject();
    cutOffTime.put("cutOffDateTimeCode", "LCO");
    rootNodeArray.add(schedule);
    assertTrue(VALIDATE_CUTOFF_TIME_CODE_PS.validate(rootNodeArray).getErrorMessages().isEmpty());

    cutOffTime.put("cutOffDateTimeCode", "LCR");
    rootNodeArray.add(schedule);
    assertFalse(VALIDATE_CUTOFF_TIME_CODE_PS.validate(rootNodeArray).getErrorMessages().isEmpty());
  }

  @Test
  void testValidateNonEmptyResponse() {
    rootNodeArray.add(objectMapper.createObjectNode());
    assertTrue(CsChecks.VALIDATE_NON_EMPTY_RESPONSE.validate(rootNodeArray).getErrorMessages().isEmpty());
    rootNodeArray.removeAll();
    assertFalse(CsChecks.VALIDATE_NON_EMPTY_RESPONSE.validate(rootNodeArray).getErrorMessages().isEmpty());
  }
}
