package org.dcsa.conformance.standards.jit.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonRebaseableContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.model.JitClassifierCode;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.PortCallServiceEventTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;
import org.dcsa.conformance.standards.jit.party.DynamicScenarioParameters;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class JitChecks {

  static final String SPECIFICATION = "specification";

  public static ActionCheck createChecksForPortCallService(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      PortCallServiceType serviceType) {
    return JsonAttribute.contentChecks(
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        List.of(checkPortCallService(serviceType), checkRightFields()));
  }

  static final Predicate<JsonNode> IS_PORT_CALL_SERVICE = node -> node.has(SPECIFICATION);

  static JsonRebaseableContentCheck checkPortCallService(PortCallServiceType serviceType) {
    return JsonAttribute.ifThen(
        "Expected Port Call Service type should match scenario (%s).".formatted(serviceType.name()),
        IS_PORT_CALL_SERVICE,
        JsonAttribute.mustEqual(
            "Check if the correct Port Call Service was supplied.",
            JsonPointer.compile("/" + SPECIFICATION + "/portCallServiceType"),
            serviceType::name));
  }

  static JsonContentCheck checkRightFields() {
    return JsonAttribute.customValidator(
        "Check if the correct PortCallServiceEventTypeCode was supplied.",
        body -> {
          if (body.has(SPECIFICATION)) {
            String actualServiceType = body.get(SPECIFICATION).get("portCallServiceType").asText();
            String portCallServiceEventTypeCode =
                body.get(SPECIFICATION).get("portCallServiceEventTypeCode").asText();
            PortCallServiceEventTypeCode code =
                PortCallServiceEventTypeCode.fromString(portCallServiceEventTypeCode);
            if (!PortCallServiceEventTypeCode.getValidPortCallServiceTypes(code)
                .contains(PortCallServiceType.fromName(actualServiceType))) {
              return Set.of(
                  "Expected matching Port Call Service type with PortCallServiceEventTypeCode. Found non-matching type: '%s' combined with code: '%s'"
                      .formatted(actualServiceType, portCallServiceEventTypeCode));
            }
          }
          return Collections.emptySet();
        });
  }

  public static ActionCheck createChecksForTimestamp(
      UUID matchedExchangeUuid, String expectedApiVersion, DynamicScenarioParameters dsp) {
    List<JsonContentCheck> checks = new ArrayList<>();
    if (dsp != null && dsp.currentTimestamp().classifierCode().equals(JitClassifierCode.PLN)) {
      checks.add(checkPlannedMatchesRequestedTimestamp(dsp));
    }
    if (checks.isEmpty()) return null;
    return JsonAttribute.contentChecks(
        x -> true, matchedExchangeUuid, HttpMessageType.REQUEST, expectedApiVersion, checks);
  }

  static JsonContentCheck checkPlannedMatchesRequestedTimestamp(DynamicScenarioParameters dsp) {
    return JsonAttribute.customValidator(
        "Check if the Planned timestamp matches the Requested timestamp.",
        body -> {
          JitTimestamp receivedTimestamp = JitTimestamp.fromJson(body);
          if (dsp.previousTimestamp().classifierCode().equals(JitClassifierCode.REQ)
              && !dsp.previousTimestamp().dateTime().equals(receivedTimestamp.dateTime())) {
            return Set.of(
                "Expected matching timestamp: '%s' but got Planned timestamp: '%s'"
                    .formatted(dsp.previousTimestamp().dateTime(), receivedTimestamp.dateTime()));
          }
          return Collections.emptySet();
        });
  }
}
