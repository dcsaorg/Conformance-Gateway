package org.dcsa.conformance.standards.cs.checks;

import static org.dcsa.conformance.standards.cs.checks.CsChecks.VALIDATE_CUTOFF_TIME_CODE;
import static org.dcsa.conformance.standards.cs.checks.CsChecks.VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP;
import static org.dcsa.conformance.standards.cs.checks.CsChecks.VALIDATE_CUTOFF_TIME_CODE_PS;
import static org.dcsa.conformance.standards.cs.checks.CsChecks.VALIDATE_PTP_CUTOFF_TIMES_AT_ROUTING_OR_LEG_LEVEL;
import static org.dcsa.conformance.standards.cs.checks.CsChecks.VALIDATE_PTP_LEG_FOOTPRINT;
import static org.dcsa.conformance.standards.cs.checks.CsChecks.VALIDATE_PTP_ROUTING_REFERENCE;
import static org.dcsa.conformance.standards.cs.checks.CsChecks.VALIDATE_PTP_SOLUTION_FOOTPRINT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.check.ConformanceErrorSeverity;
import org.junit.jupiter.api.Test;

class CsChecksTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ArrayNode rootNodeArray = objectMapper.createArrayNode();
  private final ObjectNode routing = objectMapper.createObjectNode();

  // ── VALIDATE_PTP_CUTOFF_TIMES_AT_ROUTING_OR_LEG_LEVEL ────────────────────

  @Test
  void testValidatePtpCutoffTimesAtRoutingOrLegLevel_noFieldAtAll_irrelevant() {
    ObjectNode r = objectMapper.createObjectNode();
    r.put("solutionNumber", 1);
    rootNodeArray.add(r);

    Set<ConformanceError> errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                VALIDATE_PTP_CUTOFF_TIMES_AT_ROUTING_OR_LEG_LEVEL.validate(rootNodeArray))
            .errors();
    assertEquals(1, errors.size());
    assertEquals(ConformanceErrorSeverity.IRRELEVANT, errors.iterator().next().severity());
  }

  @Test
  void testValidatePtpCutoffTimesAtRoutingOrLegLevel_emptyArray_fails() {
    ObjectNode r = objectMapper.createObjectNode();
    r.putArray("cutOffTimes"); // empty array
    rootNodeArray.add(r);

    assertFalse(
        VALIDATE_PTP_CUTOFF_TIMES_AT_ROUTING_OR_LEG_LEVEL
            .validate(rootNodeArray)
            .getErrorMessages()
            .isEmpty(),
        "Expected failure when cutOffTimes is present but empty");
  }

  @Test
  void testValidatePtpCutoffTimesAtRoutingOrLegLevel_twoRoutingsNoCutoff_irrelevant() {
    rootNodeArray.add(objectMapper.createObjectNode());
    rootNodeArray.add(objectMapper.createObjectNode());

    Set<ConformanceError> errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                VALIDATE_PTP_CUTOFF_TIMES_AT_ROUTING_OR_LEG_LEVEL.validate(rootNodeArray))
            .errors();
    assertEquals(1, errors.size());
    assertEquals(ConformanceErrorSeverity.IRRELEVANT, errors.iterator().next().severity());
  }

  @Test
  void testValidatePtpCutoffTimesAtRoutingOrLegLevel_nonEmptyAtRoutingLevel_passes() {
    ObjectNode r = objectMapper.createObjectNode();
    ArrayNode cutOffTimes = r.putArray("cutOffTimes");
    ObjectNode ct = cutOffTimes.addObject();
    ct.put("cutOffDateTimeCode", "DCO");
    ct.put("cutOffDateTime", "2025-01-10T17:00:00+01:00");
    rootNodeArray.add(r);

    assertTrue(
        VALIDATE_PTP_CUTOFF_TIMES_AT_ROUTING_OR_LEG_LEVEL
            .validate(rootNodeArray)
            .getErrorMessages()
            .isEmpty(),
        "Expected pass when cutOffTimes is non-empty at routing level");
  }

  @Test
  void testValidatePtpCutoffTimesAtRoutingOrLegLevel_nonEmptyAtLegLevel_passes() {
    ObjectNode r = objectMapper.createObjectNode();
    ArrayNode legs = r.putArray("legs");
    ObjectNode leg = legs.addObject();
    ArrayNode legCutOffTimes = leg.putArray("cutOffTimes");
    ObjectNode ct = legCutOffTimes.addObject();
    ct.put("cutOffDateTimeCode", "VCO");
    ct.put("cutOffDateTime", "2025-01-11T12:00:00+01:00");
    rootNodeArray.add(r);

    assertTrue(
        VALIDATE_PTP_CUTOFF_TIMES_AT_ROUTING_OR_LEG_LEVEL
            .validate(rootNodeArray)
            .getErrorMessages()
            .isEmpty(),
        "Expected pass when cutOffTimes is non-empty at leg level");
  }

  @Test
  void testValidatePtpCutoffTimesAtRoutingOrLegLevel_oneEmptyOneMissing_fails() {
    ObjectNode r1 = objectMapper.createObjectNode();
    r1.putArray("cutOffTimes"); // empty
    ObjectNode r2 = objectMapper.createObjectNode(); // no field
    rootNodeArray.add(r1);
    rootNodeArray.add(r2);

    assertFalse(
        VALIDATE_PTP_CUTOFF_TIMES_AT_ROUTING_OR_LEG_LEVEL
            .validate(rootNodeArray)
            .getErrorMessages()
            .isEmpty(),
        "Expected failure when field present but all arrays are empty");
  }

  @Test
  void testValidatePtpRoutingReference_absent_irrelevant() {
    ObjectNode r = objectMapper.createObjectNode();
    rootNodeArray.add(r);

    Set<ConformanceError> errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                VALIDATE_PTP_ROUTING_REFERENCE.validate(rootNodeArray))
            .errors();

    assertEquals(1, errors.size());
    assertEquals(ConformanceErrorSeverity.IRRELEVANT, errors.iterator().next().severity());
  }

  @Test
  void testValidatePtpRoutingReference_presentButEmpty_fails() {
    ObjectNode r = objectMapper.createObjectNode();
    r.put("routingReference", "");
    rootNodeArray.add(r);

    assertFalse(
        VALIDATE_PTP_ROUTING_REFERENCE.validate(rootNodeArray).getErrorMessages().isEmpty(),
        "Expected failure when routingReference is present but empty");
  }

  @Test
  void testValidatePtpRoutingReference_presentAndNonEmpty_passes() {
    ObjectNode r = objectMapper.createObjectNode();
    r.put("routingReference", "Route123");
    rootNodeArray.add(r);

    assertTrue(
        VALIDATE_PTP_ROUTING_REFERENCE.validate(rootNodeArray).getErrorMessages().isEmpty(),
        "Expected pass when routingReference is non-empty");
  }

  @Test
  void testValidatePtpSolutionFootprint_absent_irrelevant() {
    rootNodeArray.add(objectMapper.createObjectNode());

    Set<ConformanceError> errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                VALIDATE_PTP_SOLUTION_FOOTPRINT.validate(rootNodeArray))
            .errors();
    assertEquals(1, errors.size());
    assertEquals(ConformanceErrorSeverity.IRRELEVANT, errors.iterator().next().severity());
  }

  @Test
  void testValidatePtpSolutionFootprint_presentButEmpty_fails() {
    ObjectNode routingNode = objectMapper.createObjectNode();
    routingNode.putObject("solutionFootprint"); // empty object {}
    rootNodeArray.add(routingNode);

    assertFalse(
        VALIDATE_PTP_SOLUTION_FOOTPRINT.validate(rootNodeArray).getErrorMessages().isEmpty(),
        "Expected failure when solutionFootprint is present but empty");
  }

  @Test
  void testValidatePtpSolutionFootprint_presentAndNonEmpty_passes() {
    ObjectNode routingNode = objectMapper.createObjectNode();
    ObjectNode fp = routingNode.putObject("solutionFootprint");
    fp.put("co2", 12.5);
    rootNodeArray.add(routingNode);

    assertTrue(
        VALIDATE_PTP_SOLUTION_FOOTPRINT.validate(rootNodeArray).getErrorMessages().isEmpty(),
        "Expected pass when solutionFootprint is non-empty");
  }

  @Test
  void testValidatePtpLegFootprint_absent_irrelevant() {
    ObjectNode routingNode = objectMapper.createObjectNode();
    routingNode.putArray("legs").addObject(); // leg without footprint field
    rootNodeArray.add(routingNode);

    Set<ConformanceError> errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                VALIDATE_PTP_LEG_FOOTPRINT.validate(rootNodeArray))
            .errors();
    assertEquals(1, errors.size());
    assertEquals(ConformanceErrorSeverity.IRRELEVANT, errors.iterator().next().severity());
  }

  @Test
  void testValidatePtpLegFootprint_presentButEmpty_fails() {
    ObjectNode routingNode = objectMapper.createObjectNode();
    ObjectNode leg = routingNode.putArray("legs").addObject();
    leg.putObject("footprint"); // empty object {}
    rootNodeArray.add(routingNode);

    assertFalse(
        VALIDATE_PTP_LEG_FOOTPRINT.validate(rootNodeArray).getErrorMessages().isEmpty(),
        "Expected failure when leg footprint is present but empty");
  }

  @Test
  void testValidatePtpLegFootprint_presentAndNonEmpty_passes() {
    ObjectNode routingNode = objectMapper.createObjectNode();
    ObjectNode leg = routingNode.putArray("legs").addObject();
    ObjectNode fp = leg.putObject("footprint");
    fp.put("co2", 2.3);
    rootNodeArray.add(routingNode);

    assertTrue(
        VALIDATE_PTP_LEG_FOOTPRINT.validate(rootNodeArray).getErrorMessages().isEmpty(),
        "Expected pass when leg footprint is non-empty");
  }

  // ── Existing tests ────────────────────────────────────────────────────────

  @Test
  void testValidateCutoffTimeCodePtp() {
    ArrayNode cutOffTimes = routing.putArray("cutOffTimes");

    Set<ConformanceError> errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                VALIDATE_CUTOFF_TIME_CODE.validate(rootNodeArray))
            .errors();

    assertEquals(1, errors.size());
    assertEquals(ConformanceErrorSeverity.IRRELEVANT, errors.iterator().next().severity());

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

    Set<ConformanceError> errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP.validate(rootNodeArray))
            .errors();

    assertEquals(1, errors.size());
    assertEquals(ConformanceErrorSeverity.IRRELEVANT, errors.iterator().next().severity());

    cutOffTime.put("cutOffDateTimeCode", "LCO");
    routing.put("receiptTypeAtOrigin", "CO");
    assertFalse(
        VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP
            .validate(rootNodeArray)
            .getErrorMessages()
            .isEmpty());

    cutOffTime.put("cutOffDateTimeCode", "PCO");
    routing.put("receiptTypeAtOrigin", "CO");

    errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP.validate(rootNodeArray))
            .errors();
    assertEquals(1, errors.size());
    assertEquals(ConformanceErrorSeverity.IRRELEVANT, errors.iterator().next().severity());
  }

  @Test
  void testValidateCutoffTimeCodePs() {
    ObjectNode schedule = objectMapper.createObjectNode();

    Set<ConformanceError> errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                VALIDATE_CUTOFF_TIME_CODE_PS.validate(rootNodeArray))
            .errors();

    assertEquals(1, errors.size());
    assertEquals(ConformanceErrorSeverity.IRRELEVANT, errors.iterator().next().severity());

    ArrayNode vesselSchedules = schedule.putArray("vesselSchedules");
    ObjectNode vesselSchedule = vesselSchedules.addObject();

    errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                VALIDATE_CUTOFF_TIME_CODE_PS.validate(rootNodeArray))
            .errors();

    assertEquals(1, errors.size());
    assertEquals(ConformanceErrorSeverity.IRRELEVANT, errors.iterator().next().severity());

    ArrayNode cutOffTimes = vesselSchedule.putArray("cutOffTimes");
    ObjectNode cutOffTime = cutOffTimes.addObject();
    cutOffTime.put("cutOffDateTimeCode", "LCO");
    rootNodeArray.add(schedule);
    assertTrue(VALIDATE_CUTOFF_TIME_CODE_PS.validate(rootNodeArray).getErrorMessages().isEmpty());

    cutOffTime.put("cutOffDateTimeCode", "LCR");
    rootNodeArray.add(schedule);
    assertFalse(VALIDATE_CUTOFF_TIME_CODE_PS.validate(rootNodeArray).getErrorMessages().isEmpty());
  }
}
