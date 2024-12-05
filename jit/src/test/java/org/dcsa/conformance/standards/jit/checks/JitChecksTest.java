package org.dcsa.conformance.standards.jit.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.standards.jit.model.JitClassifierCode;
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
        "The value of 'specification.portCallServiceType' was 'BUNKERING' instead of 'BERTH'",
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
                    PortCallServiceType.BERTH, PortCallServiceEventTypeCode.ARRI, PortCallPhaseTypeCode.INBD))
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
        new DynamicScenarioParameters(
            null,
            createTimestamp().withClassifierCode(JitClassifierCode.REQ),
            null,
            null,
            null,
            null,
            null,
            null);
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

  private JsonNode createPortCallServiceRequest(
      PortCallServiceType serviceType,
      PortCallServiceEventTypeCode code,
      PortCallPhaseTypeCode phaseTypeCode) {
    ObjectNode specification =
        OBJECT_MAPPER
            .createObjectNode()
            .put("portCallServiceType", serviceType.name())
            .put("portCallServiceEventTypeCode", code.name());
    if (phaseTypeCode != null) {
      specification.put("portCallPhaseTypeCode", phaseTypeCode.name());
    }
    return OBJECT_MAPPER.createObjectNode().set(JitChecks.SPECIFICATION, specification);
  }

  private JitTimestamp createTimestamp() {
    return new JitTimestamp(null, null, null, JitClassifierCode.PLN, "dateTime", null, false, null);
  }
}
