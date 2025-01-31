package org.dcsa.conformance.standards.jit.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.UUID;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.jit.model.JitClassifierCode;
import org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.PortCallPhaseTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceEventTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;
import org.dcsa.conformance.standards.jit.party.DynamicScenarioParameters;
import org.junit.jupiter.api.Test;

class JitChecksTest {

  @Test
  void checkPortCallService() {
    JsonNode request =
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
        JitChecks.checkIDsMatchesPreviousCall(
                new DynamicScenarioParameters()) // No previous data captured
            .validate(OBJECT_MAPPER.createObjectNode())
            .isEmpty());
    assertTrue(
        JitChecks.checkIDsMatchesPreviousCall(dsp)
            .validate(
                OBJECT_MAPPER.createObjectNode().put(JitChecks.PORT_CALL_ID, dsp.portCallID()))
            .isEmpty());
    assertTrue(
        JitChecks.checkIDsMatchesPreviousCall(dsp)
            .validate(
                OBJECT_MAPPER
                    .createObjectNode()
                    .put(JitChecks.PORT_CALL_SERVICE_ID, dsp.portCallServiceID()))
            .isEmpty());
    assertTrue(
        JitChecks.checkIDsMatchesPreviousCall(dsp)
            .validate(
                OBJECT_MAPPER
                    .createObjectNode()
                    .put(JitChecks.TERMINAL_CALL_ID, dsp.terminalCallID()))
            .isEmpty());

    // Non-matching IDs used
    assertEquals(
        3,
        JitChecks.checkIDsMatchesPreviousCall(dsp)
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

  private JsonNode createPortCallServiceRequest(
      PortCallServiceType serviceType,
      PortCallServiceEventTypeCode code,
      PortCallPhaseTypeCode phaseTypeCode) {
    ObjectNode jsonNode =
        (ObjectNode)
            JsonToolkit.templateFileToJsonNode(
                "/standards/jit/messages/jit-200-port-call-service-request.json",
                Map.of(
                    "PORT_CALL_SERVICE_TYPE_PLACEHOLDER",
                    serviceType.name(),
                    "PORT_CALL_SERVICE_EVENT_TYPE_CODE_PLACEHOLDER",
                    code.name(),
                    "PORT_CALL_PHASE_TYPE_CODE_PLACEHOLDER",
                    phaseTypeCode == null ? "" : phaseTypeCode.name(),
                    "IS_FYI_PLACEHOLDER",
                    "false"));

    if (phaseTypeCode == null) {
      jsonNode.remove("portCallPhaseTypeCode");
    }
    return jsonNode;
  }

  private JitTimestamp createTimestamp() {
    return new JitTimestamp(null, null, null, JitClassifierCode.PLN, "dateTime", null, false, null);
  }
}
