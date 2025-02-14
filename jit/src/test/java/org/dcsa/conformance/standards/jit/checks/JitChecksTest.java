package org.dcsa.conformance.standards.jit.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.dcsa.conformance.standards.jit.model.JitClassifierCode;
import org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.PortCallPhaseTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceEventTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceLocationTimestamp;
import org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode;
import org.dcsa.conformance.standards.jit.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.jit.party.JitPartyHelper;
import org.junit.jupiter.api.Test;

class JitChecksTest {

  @Test
  void checkPortCallService() {
    ObjectNode request =
        createPortCallServiceRequest(
            PortCallServiceTypeCode.BERTH, PortCallServiceEventTypeCode.ARRI, null);
    assertTrue(JitChecks.IS_PORT_CALL_SERVICE.test(request));
    assertTrue(
        JitChecks.checkPortCallService(PortCallServiceTypeCode.BERTH).validate(request).isEmpty());

    assertEquals(
        "The value of 'portCallServiceTypeCode' was 'BUNKERING' instead of 'BERTH'",
        JitChecks.checkPortCallService(PortCallServiceTypeCode.BERTH)
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceTypeCode.BUNKERING, PortCallServiceEventTypeCode.ARRI, null))
            .iterator()
            .next());
  }

  @Test
  void checkPortCallServiceMoves() {
    ObjectNode nonMovesRequest =
        createPortCallServiceRequest(
            PortCallServiceTypeCode.BERTH, PortCallServiceEventTypeCode.ARRI, null);
    // Moves checks
    assertTrue(JitChecks.checkPortCallServiceHasMoves(false).validate(nonMovesRequest).isEmpty());
    assertFalse(JitChecks.checkPortCallServiceHasMoves(true).validate(nonMovesRequest).isEmpty());

    ObjectNode movesRequest =
        createPortCallServiceRequest(
            PortCallServiceTypeCode.MOVES, PortCallServiceEventTypeCode.ARRI, null);
    assertFalse(JitChecks.checkPortCallServiceHasMoves(false).validate(movesRequest).isEmpty());
    assertTrue(JitChecks.checkPortCallServiceHasMoves(true).validate(movesRequest).isEmpty());

    assertTrue(JitChecks.MOVES_OBJECTS_VERIFY_CARRIER_CODES.validate(movesRequest).isEmpty());

    // Verify: Max only one `moves` object without a 'carrierCode'
    movesRequest.get("moves").forEach(jsonNode -> ((ObjectNode) jsonNode).remove("carrierCode"));
    assertFalse(JitChecks.MOVES_OBJECTS_VERIFY_CARRIER_CODES.validate(movesRequest).isEmpty());

    // Verify: If there are multiple `moves` objects, a `carrierCode` MUST NOT be repeated.
    movesRequest
        .get("moves")
        .forEach(jsonNode -> ((ObjectNode) jsonNode).put("carrierCode", "NVOCC"));
    assertEquals(
        "Expected carrierCodes to be different in the given moves objects; found multiple are the same!",
        JitChecks.MOVES_OBJECTS_VERIFY_CARRIER_CODES.validate(movesRequest).iterator().next());

    // Verify 2 different carrierCodes are allowed
    ((ObjectNode) movesRequest.get("moves").get(0)).put("carrierCode", "ABCD");
    assertTrue(JitChecks.MOVES_OBJECTS_VERIFY_CARRIER_CODES.validate(movesRequest).isEmpty());
  }

  @Test
  void checkTimestamps() {
    // replyToTimestampID: Condition: Only applicable after initial **Timestamp** has been sent.
    assertTrue(
        JitChecks.checkTimestampReplyTimestampIDisAbsent()
            .validate(createTimestamp().toJson())
            .isEmpty());
    assertFalse(
        JitChecks.checkTimestampReplyTimestampIDisAbsent()
            .validate(createTimestamp().toJson().put("replyToTimestampID", "something"))
            .isEmpty());

    // replyToTimestampID matches the previous timestamp
    var previousTimestamp = UUID.randomUUID().toString();
    var dsp =
        new DynamicScenarioParameters()
            .withPreviousTimestamp(createTimestamp().withTimestampID(previousTimestamp));
    // Assert Matching replyToTimestampID
    assertTrue(
        JitChecks.checkTimestampIDsMatchesPreviousCall(dsp)
            .validate(createTimestamp().withReplyToTimestampID(previousTimestamp).toJson())
            .isEmpty());

    // Assert No previous timestamp
    assertTrue(
        JitChecks.checkTimestampIDsMatchesPreviousCall(new DynamicScenarioParameters())
            .validate(createTimestamp().toJson())
            .isEmpty());

    // Assert Non-matching replyToTimestampID
    dsp = dsp.withPreviousTimestamp(dsp.previousTimestamp().withTimestampID("somethingElse"));
    assertFalse(
        JitChecks.checkTimestampIDsMatchesPreviousCall(dsp)
            .validate(createTimestamp().withReplyToTimestampID(previousTimestamp).toJson())
            .isEmpty());
  }

  @Test
  void checkPortCallServiceEventTypeCodeValues() {
    assertTrue(
        JitChecks.checkRightFieldValues()
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceTypeCode.BERTH,
                    PortCallServiceEventTypeCode.ARRI,
                    PortCallPhaseTypeCode.INBD))
            .isEmpty());

    assertEquals(
        "Expected matching Port Call Service type with PortCallServiceEventTypeCode. Found non-matching type: 'SLUDGE' combined with code: 'DEPA'",
        JitChecks.checkRightFieldValues()
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceTypeCode.SLUDGE, PortCallServiceEventTypeCode.DEPA, null))
            .iterator()
            .next());
  }

  @Test
  void checkPlannedMatchesRequestedTimestamp() {
    DynamicScenarioParameters dsp =
        new DynamicScenarioParameters()
            .withPreviousTimestamp(createTimestamp().withClassifierCode(JitClassifierCode.REQ));
    assertTrue(
        JitChecks.checkPlannedMatchesRequestedTimestamp(dsp)
            .validate(createTimestamp().toJson())
            .isEmpty());

    assertEquals(
        "Expected matching timestamp: 'dateTime' but got Planned timestamp: 'somethingElse'",
        JitChecks.checkPlannedMatchesRequestedTimestamp(dsp)
            .validate(createTimestamp().withDateTime("somethingElse").toJson())
            .iterator()
            .next());
  }

  @Test
  void checkScenarioServiceTypeMatchesScenarioType_FULL_ERP() {
    DynamicScenarioParameters dsp =
        new DynamicScenarioParameters().withSelector(JitServiceTypeSelector.FULL_ERP);
    assertTrue(
        JitChecks.checkPortCallServiceRightType(dsp)
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceTypeCode.BERTH,
                    PortCallServiceEventTypeCode.ARRI,
                    PortCallPhaseTypeCode.INBD))
            .isEmpty());

    assertEquals(
        "Expected matching Port Call Service type with scenario 'full ERP'. Found non-matching type: 'SEA_PASSAGE'",
        JitChecks.checkPortCallServiceRightType(dsp)
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceTypeCode.SEA_PASSAGE,
                    PortCallServiceEventTypeCode.ARRI,
                    PortCallPhaseTypeCode.INBD))
            .iterator()
            .next());
  }

  @Test
  void checkScenarioServiceTypeMatchesScenarioType_S_A_Pattern() {
    DynamicScenarioParameters dsp =
        new DynamicScenarioParameters().withSelector(JitServiceTypeSelector.S_A_PATTERN);
    assertTrue(
        JitChecks.checkPortCallServiceRightType(dsp)
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceTypeCode.ALL_FAST,
                    PortCallServiceEventTypeCode.ARRI,
                    PortCallPhaseTypeCode.INBD))
            .isEmpty());

    assertEquals(
        "Expected matching Port Call Service type with scenario 'S-A pattern'. Found non-matching type: 'SLUDGE'",
        JitChecks.checkPortCallServiceRightType(dsp)
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceTypeCode.SLUDGE,
                    PortCallServiceEventTypeCode.ARRI,
                    PortCallPhaseTypeCode.INBD))
            .iterator()
            .next());
  }

  @Test
  void checkIDsMatchesPreviousActions() {
    DynamicScenarioParameters dsp =
        new DynamicScenarioParameters(
            null,
            null,
            null,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            null,
            false);
    // Happy path: reuse the IDs from the DSP, captured data from previous actions
    assertTrue(
        JitChecks.checkCallIDMatchPreviousCallID(
                new DynamicScenarioParameters()) // No previous data captured
            .validate(OBJECT_MAPPER.createObjectNode())
            .isEmpty());
    assertTrue(
        JitChecks.checkCallIDMatchPreviousCallID(dsp)
            .validate(
                OBJECT_MAPPER.createObjectNode().put(JitChecks.PORT_CALL_ID, dsp.portCallID()))
            .isEmpty());
    assertTrue(
        JitChecks.checkCallIDMatchPreviousCallID(dsp)
            .validate(
                OBJECT_MAPPER
                    .createObjectNode()
                    .put(JitChecks.PORT_CALL_SERVICE_ID, dsp.portCallServiceID()))
            .isEmpty());
    assertTrue(
        JitChecks.checkCallIDMatchPreviousCallID(dsp)
            .validate(
                OBJECT_MAPPER
                    .createObjectNode()
                    .put(JitChecks.TERMINAL_CALL_ID, dsp.terminalCallID()))
            .isEmpty());

    // Non-matching IDs used
    assertEquals(
        3,
        JitChecks.checkCallIDMatchPreviousCallID(dsp)
            .validate(
                OBJECT_MAPPER
                    .createObjectNode()
                    .put(JitChecks.TERMINAL_CALL_ID, "wrong-id")
                    .put(JitChecks.PORT_CALL_ID, "wrong-id")
                    .put(JitChecks.PORT_CALL_SERVICE_ID, "wrong-id"))
            .size());
  }

  @Test
  void testIsFYICheck() {
    assertEquals(
        1,
        JitChecks.IS_FYI_TRUE
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceTypeCode.BERTH, PortCallServiceEventTypeCode.ARRI, null))
            .size());

    assertFalse(JitChecks.IS_FYI_TRUE.validate(createTimestamp().toJson()).isEmpty());
    assertTrue(JitChecks.IS_FYI_TRUE.validate(createTimestamp().withFYI(true).toJson()).isEmpty());
  }

  @Test
  void testMovesCarrierCodeImpliesCarrierCodeListProvider() {
    ObjectNode request =
        createPortCallServiceRequest(
            PortCallServiceTypeCode.MOVES, PortCallServiceEventTypeCode.ARRI, null);

    assertTrue(
        JitChecks.MOVES_CARRIER_CODE_IMPLIES_CARRIER_CODE_LIST_PROVIDER
            .validate(request)
            .isEmpty());

    // Add one carrierCode without the listProvider.
    // Remove the carrierCodeListProvider and verify 2 errors are returned
    ((ObjectNode) request.get("moves").get(1)).put("carrierCode", "NVOCC");
    request
        .get("moves")
        .forEach(jsonNode -> ((ObjectNode) jsonNode).remove("carrierCodeListProvider"));
    assertEquals(
        2,
        JitChecks.MOVES_CARRIER_CODE_IMPLIES_CARRIER_CODE_LIST_PROVIDER.validate(request).size());

    // Remove the carrierCode occurrence and verify all is good
    request.get("moves").forEach(jsonNode -> ((ObjectNode) jsonNode).remove("carrierCode"));
    assertEquals(
        0,
        JitChecks.MOVES_CARRIER_CODE_IMPLIES_CARRIER_CODE_LIST_PROVIDER.validate(request).size());
  }

  @Test
  void testMovesCarrierCodeListProviderImpliesCarrierCode() {
    ObjectNode request =
        createPortCallServiceRequest(
            PortCallServiceTypeCode.MOVES, PortCallServiceEventTypeCode.ARRI, null);

    assertTrue(
        JitChecks.MOVES_CARRIER_CODE_LIST_PROVIDER_IMPLIES_CARRIER_CODE
            .validate(request)
            .isEmpty());

    // Remove the carrierCodeListProvider occurrence; add one carrierCodeListProvider and verify 2
    // errors are returned
    ((ObjectNode) request.get("moves").get(0)).remove("carrierCode");
    ((ObjectNode) request.get("moves").get(1)).put("carrierCodeListProvider", "NMFTA");
    assertEquals(
        2,
        JitChecks.MOVES_CARRIER_CODE_LIST_PROVIDER_IMPLIES_CARRIER_CODE.validate(request).size());

    // Remove all carrierCode occurrences and verify all is good
    request
        .get("moves")
        .forEach(jsonNode -> ((ObjectNode) jsonNode).remove("carrierCodeListProvider"));
    assertEquals(
        0,
        JitChecks.MOVES_CARRIER_CODE_LIST_PROVIDER_IMPLIES_CARRIER_CODE.validate(request).size());
  }

  @Test
  void testPortCallVesselValidation() {
    var portCall = createPortCall();
    assertTrue(
        JitChecks.VESSEL_NEEDS_ONE_OF_VESSEL_IMO_NUMBER_OR_MMSI_NUMBER
            .validate(portCall)
            .isEmpty());

    // Remove vesselIMONumber and verify still valid
    ((ObjectNode) portCall.required("vessel")).remove("vesselIMONumber");
    assertTrue(
        JitChecks.VESSEL_NEEDS_ONE_OF_VESSEL_IMO_NUMBER_OR_MMSI_NUMBER
            .validate(portCall)
            .isEmpty());

    // Remove MMSINumber as well and verify invalid
    ((ObjectNode) portCall.required("vessel")).remove("MMSINumber");
    assertFalse(
        JitChecks.VESSEL_NEEDS_ONE_OF_VESSEL_IMO_NUMBER_OR_MMSI_NUMBER
            .validate(portCall)
            .isEmpty());
  }

  @Test
  void testPortCallHasDimensionUnitWhenRequired() {
    var portCall = createPortCall();
    assertTrue(
        JitChecks.VESSEL_WIDTH_OR_LENGTH_OVERALL_REQUIRES_DIMENSION_UNIT
            .validate(portCall)
            .isEmpty());

    // Remove lengthOverall and width and verify still valid
    ((ObjectNode) portCall.required("vessel")).remove("lengthOverall");
    assertTrue(
        JitChecks.VESSEL_WIDTH_OR_LENGTH_OVERALL_REQUIRES_DIMENSION_UNIT
            .validate(portCall)
            .isEmpty());

    ((ObjectNode) portCall.required("vessel")).remove("width");
    assertTrue(
        JitChecks.VESSEL_WIDTH_OR_LENGTH_OVERALL_REQUIRES_DIMENSION_UNIT
            .validate(portCall)
            .isEmpty());

    // Remove dimensionUnit and verify invalid
    portCall = createPortCall();
    ((ObjectNode) portCall.required("vessel")).remove("dimensionUnit");
    assertFalse(
        JitChecks.VESSEL_WIDTH_OR_LENGTH_OVERALL_REQUIRES_DIMENSION_UNIT
            .validate(portCall)
            .isEmpty());
  }

  @Test
  void testVesselStatus() {
    var vesselStatus = createVesselStatus();
    // Happy path
    assertTrue(JitChecks.VESSELSTATUS_DRAFTS_NEED_DIMENSION_UNIT.validate(vesselStatus).isEmpty());
    vesselStatus.remove("draft");
    assertTrue(JitChecks.VESSELSTATUS_DRAFTS_NEED_DIMENSION_UNIT.validate(vesselStatus).isEmpty());
    vesselStatus.remove("airDraft");
    assertTrue(JitChecks.VESSELSTATUS_DRAFTS_NEED_DIMENSION_UNIT.validate(vesselStatus).isEmpty());

    // Remove dimensionUnit and verify invalid
    vesselStatus = createVesselStatus();
    vesselStatus.remove("dimensionUnit");
    assertFalse(JitChecks.VESSELSTATUS_DRAFTS_NEED_DIMENSION_UNIT.validate(vesselStatus).isEmpty());
    vesselStatus.remove("draft");
    assertFalse(JitChecks.VESSELSTATUS_DRAFTS_NEED_DIMENSION_UNIT.validate(vesselStatus).isEmpty());

    // Now all required fields are removed, should be good again.
    vesselStatus.remove("airDraft");
    assertTrue(JitChecks.VESSELSTATUS_DRAFTS_NEED_DIMENSION_UNIT.validate(vesselStatus).isEmpty());
  }

  @Test
  void testFromTimestampWithOptionalFields() {
    ObjectNode jsonBody = createTimestamp().toJson();
    var portCallServiceLocation = OBJECT_MAPPER.createObjectNode();
    portCallServiceLocation
        .put("locationName", "CMP Container Terminal Copenhagen")
        .put("UNLocationCode", "DKCPH")
        .put("geoCoordinate", "55.7036,12.5951");
    portCallServiceLocation.set(
        "facility", OBJECT_MAPPER.createObjectNode().put("facilityCode", "DKCPH"));
    jsonBody.set("portCallServiceLocation", portCallServiceLocation);
    // Validate all given data fits into POJO
    JitTimestamp timestamp = JitTimestamp.fromJson(jsonBody);
    assertNotNull(timestamp);
  }

  @Test
  void testTimestampAllowedToHavePortCallServiceLocation() {
    var timestamp =
        createTimestamp()
            .withPortCallServiceLocation(new PortCallServiceLocationTimestamp("name", "code"));
    // validate it does not work with PLN.
    assertFalse(
        JitChecks.TIMESTAMP_ALLOWS_PORT_CALL_SERVICE_LOCATION
            .validate(timestamp.toJson())
            .isEmpty());
    // validate it only works with REQ.
    timestamp = timestamp.withClassifierCode(JitClassifierCode.REQ);
    assertTrue(
        JitChecks.TIMESTAMP_ALLOWS_PORT_CALL_SERVICE_LOCATION
            .validate(timestamp.toJson())
            .isEmpty());

    // validate it only works with UNLocationCode (being filled).
    assertTrue(
        JitChecks.TIMESTAMP_VALIDATE_PORT_CALL_SERVICE_LOCATION
            .validate(timestamp.toJson())
            .isEmpty());
    timestamp =
        timestamp.withPortCallServiceLocation(new PortCallServiceLocationTimestamp("name", null));
    assertFalse(
        JitChecks.TIMESTAMP_VALIDATE_PORT_CALL_SERVICE_LOCATION
            .validate(timestamp.toJson())
            .isEmpty());
  }

  @Test
  void testCheckExpectedResultCount() {
    ObjectNode timestamp = createTimestamp().toJson();
    ArrayNode body = OBJECT_MAPPER.createArrayNode().add(timestamp).add(timestamp);
    assertEquals(
        "Expected 1 result(s), but got 2 result(s).",
        JitChecks.checkExpectedResultCount(1, false).validate(body).iterator().next());
    assertTrue(JitChecks.checkExpectedResultCount(2, false).validate(body).isEmpty());

    // Assert that moreResultsAllowed is respected
    body.add(timestamp).add(timestamp);
    assertTrue(JitChecks.checkExpectedResultCount(2, true).validate(body).isEmpty());
    assertTrue(JitChecks.checkExpectedResultCount(4, true).validate(body).isEmpty());
    assertFalse(JitChecks.checkExpectedResultCount(5, true).validate(body).isEmpty());
  }

  private ObjectNode createPortCall() {
    var dsp =
        new DynamicScenarioParameters(
            null, null, null, UUID.randomUUID().toString(), null, null, null, false);
    return JitPartyHelper.getFileWithReplacedPlaceHolders("port-call", dsp);
  }

  private ObjectNode createVesselStatus() {
    var dsp =
        new DynamicScenarioParameters(
            null, null, null, UUID.randomUUID().toString(), null, null, null, false);
    return JitPartyHelper.getFileWithReplacedPlaceHolders("vessel-status", dsp);
  }

  private ObjectNode createPortCallServiceRequest(
      PortCallServiceTypeCode serviceType,
      PortCallServiceEventTypeCode code,
      PortCallPhaseTypeCode phaseTypeCode) {
    var dsp =
        new DynamicScenarioParameters(
            null, null, serviceType, null, null, UUID.randomUUID().toString(), null, false);
    ObjectNode node = JitPartyHelper.getFileWithReplacedPlaceHolders("port-call-service", dsp);

    if (code != null) node.put("portCallServiceEventTypeCode", code.name());
    if (phaseTypeCode != null) node.put("portCallPhaseTypeCode", phaseTypeCode.name());
    if (serviceType != PortCallServiceTypeCode.MOVES) {
      node.remove("moves");
    }
    return node;
  }

  private JitTimestamp createTimestamp() {
    return new JitTimestamp(
        null, null, null, JitClassifierCode.PLN, "dateTime", null, null, false, null);
  }
}
