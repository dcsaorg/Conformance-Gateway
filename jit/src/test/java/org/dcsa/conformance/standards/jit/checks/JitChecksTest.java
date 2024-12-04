package org.dcsa.conformance.standards.jit.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.standards.jit.model.JitClassifierCode;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.PortCallServiceEventTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;
import org.dcsa.conformance.standards.jit.party.DynamicScenarioParameters;
import org.junit.jupiter.api.Test;

class JitChecksTest {

  @Test
  void checkPortCallService() {
    JsonNode request =
        createPortCallServiceRequest(PortCallServiceType.BERTH, PortCallServiceEventTypeCode.ARRI);
    assertTrue(JitChecks.IS_PORT_CALL_SERVICE.test(request));
    assertTrue(JitChecks.checkPortCallService(PortCallServiceType.BERTH).validate(request).isEmpty());

    assertEquals(
        "The value of 'specification.portCallServiceType' was 'BUNKERING' instead of 'BERTH'",
        JitChecks.checkPortCallService(PortCallServiceType.BERTH)
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceType.BUNKERING, PortCallServiceEventTypeCode.ARRI))
            .iterator()
            .next());
  }

  @Test
  void checkRightFields() {
    assertTrue(
        JitChecks.checkRightFields()
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceType.BERTH, PortCallServiceEventTypeCode.ARRI))
            .isEmpty());

    assertEquals(
        "Expected matching Port Call Service type with PortCallServiceEventTypeCode. Found non-matching type: 'SLUDGE' combined with code: 'DEPA'",
        JitChecks.checkRightFields()
            .validate(
                createPortCallServiceRequest(
                    PortCallServiceType.SLUDGE, PortCallServiceEventTypeCode.DEPA))
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
      PortCallServiceType serviceType, PortCallServiceEventTypeCode code) {
    return OBJECT_MAPPER
        .createObjectNode()
        .set(
            JitChecks.SPECIFICATION,
            OBJECT_MAPPER
                .createObjectNode()
                .put("portCallServiceType", serviceType.name())
                .put("portCallServiceEventTypeCode", code.name()));
  }

  private JitTimestamp createTimestamp() {
    return new JitTimestamp(null, null, null, JitClassifierCode.PLN, "dateTime", null, false, null);
  }
}
