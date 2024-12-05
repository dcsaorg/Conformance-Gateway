package org.dcsa.conformance.standards.jit.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonRebaseableContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.model.JitClassifierCode;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.PortCallPhaseTypeCode;
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
        List.of(checkPortCallService(serviceType), checkRightFieldValues()));
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

  static JsonContentCheck checkRightFieldValues() {
    return JsonAttribute.customValidator(
        "Check if valid combinations of values are supplied.",
        body -> {
          Set<String> issues = new HashSet<>();
          if (IS_PORT_CALL_SERVICE.test(body)) {
            String actualServiceType = body.get(SPECIFICATION).get("portCallServiceType").asText();
            issues.add(verifyPortCallServiceEventTypeCode(body, actualServiceType));
            issues.add(verifyPortCallPhaseTypeCode(body, actualServiceType));
          }
          return issues.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        });
  }

  private static String verifyPortCallPhaseTypeCode(JsonNode body, String actualServiceType) {
    String portCallPhaseTypeCode = body.get(SPECIFICATION).path("portCallPhaseTypeCode").asText("");
    if (!PortCallPhaseTypeCode.isValidCombination(
            PortCallServiceType.fromName(actualServiceType), portCallPhaseTypeCode)) {
      return "Expected matching Port Call Service type with PortCallPhaseTypeCode. Found non-matching type: '%s' combined with code: '%s'"
          .formatted(actualServiceType, portCallPhaseTypeCode);
    }
    return null;
  }

  private static String verifyPortCallServiceEventTypeCode(
      JsonNode body, String actualServiceType) {
    String portCallServiceEventTypeCode =
        body.get(SPECIFICATION).get("portCallServiceEventTypeCode").asText();
    if (!PortCallServiceEventTypeCode.isValidCombination(
        PortCallServiceType.fromName(actualServiceType), portCallServiceEventTypeCode)) {
      return "Expected matching Port Call Service type with PortCallServiceEventTypeCode. Found non-matching type: '%s' combined with code: '%s'"
          .formatted(actualServiceType, portCallServiceEventTypeCode);
    }
    return null;
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
