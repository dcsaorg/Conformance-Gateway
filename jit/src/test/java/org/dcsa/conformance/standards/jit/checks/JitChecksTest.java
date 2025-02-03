package org.dcsa.conformance.standards.jit.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.dcsa.conformance.standards.jit.model.JitClassifierCode;
import org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.PortCallPhaseTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceEventTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;
import org.dcsa.conformance.standards.jit.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.jit.party.JitPartyHelper;
import org.junit.jupiter.api.Test;

class JitChecksTest {

  @Test
  void checkPortCallService() {
    ObjectNode request =
        createPortCallServiceRequest(
            PortCallServiceType.BERTH, PortCallServiceEventTypeCode.ARRI, null);
    assertTrue(JitChecks.IS_PORT_CALL_SERVICE.test(request));
    assertTrue(
        JitChecks.checkPortCallService(PortCallServiceType.BERTH).validate(request).isEmpty());

    assertEquals(
        "The value of 'portCallServiceType' was 'BUNKERING' instead of 'BERTH'",
        JitChecks.checkPortCallService(PortCallServiceType.BERTH)
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceType.BUNKERING, PortCallServiceEventTypeCode.ARRI, null))
            .iterator()
            .next());

    assertTrue(JitChecks.checkPortCallServiceHasMoves(false).validate(request).isEmpty());
    assertFalse(JitChecks.checkPortCallServiceHasMoves(true).validate(request).isEmpty());
    request.put("moves", "BERTH");
    assertFalse(JitChecks.checkPortCallServiceHasMoves(false).validate(request).isEmpty());
    assertTrue(JitChecks.checkPortCallServiceHasMoves(true).validate(request).isEmpty());
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
                    PortCallServiceType.BERTH,
                    PortCallServiceEventTypeCode.ARRI,
                    PortCallPhaseTypeCode.INBD))
            .isEmpty());

    assertEquals(
        "Expected matching Port Call Service type with PortCallServiceEventTypeCode. Found non-matching type: 'SLUDGE' combined with code: 'DEPA'",
        JitChecks.checkRightFieldValues()
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceType.SLUDGE, PortCallServiceEventTypeCode.DEPA, null))
            .iterator()
            .next());
  }

  @Test
  void checkPortCallPhaseTypeCodeValues() {
    assertTrue(
        JitChecks.checkRightFieldValues()
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceType.BERTH,
                    PortCallServiceEventTypeCode.ARRI,
                    PortCallPhaseTypeCode.ALGS))
            .isEmpty());

    assertEquals(
        "Expected matching Port Call Service type with PortCallPhaseTypeCode. Found non-matching type: 'SLUDGE' combined with code: 'SHIF'",
        JitChecks.checkRightFieldValues()
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceType.SLUDGE,
                    PortCallServiceEventTypeCode.DEPA,
                    PortCallPhaseTypeCode.SHIF))
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
                    PortCallServiceType.BERTH,
                    PortCallServiceEventTypeCode.ARRI,
                    PortCallPhaseTypeCode.INBD))
            .isEmpty());

    assertEquals(
        "Expected matching Port Call Service type with scenario 'full ERP'. Found non-matching type: 'SEA_PASSAGE'",
        JitChecks.checkPortCallServiceRightType(dsp)
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceType.SEA_PASSAGE,
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
                    PortCallServiceType.ALL_FAST,
                    PortCallServiceEventTypeCode.ARRI,
                    PortCallPhaseTypeCode.INBD))
            .isEmpty());

    assertEquals(
        "Expected matching Port Call Service type with scenario 'S-A pattern'. Found non-matching type: 'SLUDGE'",
        JitChecks.checkPortCallServiceRightType(dsp)
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceType.SLUDGE,
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
                    PortCallServiceType.BERTH, PortCallServiceEventTypeCode.ARRI, null))
            .size());

    assertFalse(JitChecks.IS_FYI_TRUE.validate(createTimestamp().toJson()).isEmpty());
    assertTrue(JitChecks.IS_FYI_TRUE.validate(createTimestamp().withFYI(true).toJson()).isEmpty());
  }

  @Test
  void testMovesCarrierCodeImpliesCarrierCodeListProvider() {
    ObjectNode request =
        createPortCallServiceRequest(
            PortCallServiceType.MOVES, PortCallServiceEventTypeCode.ARRI, null);

    assertTrue(
        JitChecks.MOVES_CARRIER_CODE_IMPLIES_CARRIER_CODE_LIST_PROVIDER
            .validate(request)
            .isEmpty());

    // Remove all carrierCodeListProvider occurrences and verify 2 errors are returned
    request
        .get("moves")
        .forEach(jsonNode -> ((ObjectNode) jsonNode).remove("carrierCodeListProvider"));
    assertEquals(
        2,
        JitChecks.MOVES_CARRIER_CODE_IMPLIES_CARRIER_CODE_LIST_PROVIDER.validate(request).size());

    // Remove all carrierCode occurrences and verify all is good
    request.get("moves").forEach(jsonNode -> ((ObjectNode) jsonNode).remove("carrierCode"));
    assertEquals(
        0,
        JitChecks.MOVES_CARRIER_CODE_IMPLIES_CARRIER_CODE_LIST_PROVIDER.validate(request).size());
  }

  @Test
  void testMovesCarrierCodeListProviderImpliesCarrierCode() {
    ObjectNode request =
        createPortCallServiceRequest(
            PortCallServiceType.MOVES, PortCallServiceEventTypeCode.ARRI, null);

    assertTrue(
        JitChecks.MOVES_CARRIER_CODE_LIST_PROVIDER_IMPLIES_CARRIER_CODE
            .validate(request)
            .isEmpty());

    // Remove all carrierCodeListProvider occurrences and verify 2 errors are returned
    request.get("moves").forEach(jsonNode -> ((ObjectNode) jsonNode).remove("carrierCode"));
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
        JitChecks.VESSEL_LENGTH_OVERALL_REQUIRES_DIMENSION_UNIT.validate(portCall).isEmpty());
    assertTrue(JitChecks.VESSEL_WIDTH_REQUIRES_DIMENSION_UNIT.validate(portCall).isEmpty());

    // Remove lengthOverall and width and verify still valid
    ((ObjectNode) portCall.required("vessel")).remove("lengthOverall");
    assertTrue(
        JitChecks.VESSEL_LENGTH_OVERALL_REQUIRES_DIMENSION_UNIT.validate(portCall).isEmpty());

    ((ObjectNode) portCall.required("vessel")).remove("width");
    assertTrue(JitChecks.VESSEL_WIDTH_REQUIRES_DIMENSION_UNIT.validate(portCall).isEmpty());

    // Remove dimensionUnit and verify invalid
    portCall = createPortCall();
    ((ObjectNode) portCall.required("vessel")).remove("dimensionUnit");
    assertFalse(
        JitChecks.VESSEL_LENGTH_OVERALL_REQUIRES_DIMENSION_UNIT.validate(portCall).isEmpty());
    assertFalse(JitChecks.VESSEL_WIDTH_REQUIRES_DIMENSION_UNIT.validate(portCall).isEmpty());
  }

  private ObjectNode createPortCall() {
    var dsp =
        new DynamicScenarioParameters(
            null, null, null, UUID.randomUUID().toString(), null, null, null, false);
    return JitPartyHelper.getFileWithReplacedPlaceHolders("port-call", dsp);
  }

  private ObjectNode createPortCallServiceRequest(
      PortCallServiceType serviceType,
      PortCallServiceEventTypeCode code,
      PortCallPhaseTypeCode phaseTypeCode) {
    var dsp =
        new DynamicScenarioParameters(
            null, null, serviceType, null, null, UUID.randomUUID().toString(), null, false);
    ObjectNode node = JitPartyHelper.getFileWithReplacedPlaceHolders("port-call-service", dsp);

    if (code != null) node.put("portCallServiceEventTypeCode", code.name());
    if (phaseTypeCode != null) node.put("portCallPhaseTypeCode", phaseTypeCode.name());
    if (serviceType != PortCallServiceType.MOVES) {
      node.remove("moves");
    }
    return node;
  }

  private JitTimestamp createTimestamp() {
    return new JitTimestamp(null, null, null, JitClassifierCode.PLN, "dateTime", null, false, null);
  }
}
