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
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonRebaseableContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.model.JitClassifierCode;
import org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.PortCallServiceEventTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode;
import org.dcsa.conformance.standards.jit.party.DynamicScenarioParameters;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class JitChecks {

  public static final String TERMINAL_CALL_ID = "terminalCallID";
  public static final String PORT_CALL_ID = "portCallID";
  public static final String PORT_CALL_SERVICE_ID = "portCallServiceID";
  public static final String TIMESTAMP_ID = "timestampID";
  public static final String CLASSIFIER_CODE = "classifierCode";
  public static final String PORT_CALL_SERVICE_TYPE = "portCallServiceTypeCode";
  public static final String MOVES = "moves";
  public static final String CARRIER_CODE = "carrierCode";

  static final JsonRebaseableContentCheck MOVES_CARRIER_CODE_IMPLIES_CARRIER_CODE_LIST_PROVIDER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The moves.carrierCode implies moves.carrierCodeListProvider",
          mav -> mav.submitAllMatching("moves.*"),
          JsonAttribute.presenceImpliesOtherField(CARRIER_CODE, "carrierCodeListProvider"));

  static final JsonRebaseableContentCheck MOVES_CARRIER_CODE_LIST_PROVIDER_IMPLIES_CARRIER_CODE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The moves.carrierCodeListProvider implies moves.carrierCode",
          mav -> mav.submitAllMatching("moves.*"),
          JsonAttribute.presenceImpliesOtherField("carrierCodeListProvider", CARRIER_CODE));

  static final JsonContentCheck MOVES_OBJECTS_VERIFY_CARRIER_CODES =
      JsonAttribute.customValidator(
          "Max only one `moves` object without a 'carrierCode' & all 'carrierCode' values should be different.",
          body -> {
            JsonNode moves = body.path(MOVES);
            if (!moves.isMissingNode() && moves.isArray() && moves.size() > 1) {
              List<String> carrierCodes = new ArrayList<>();
              int emptyCount = 0;
              for (JsonNode move : moves) {
                if (move.has(CARRIER_CODE)) {
                  carrierCodes.add(move.path(CARRIER_CODE).asText());
                } else emptyCount++;
              }
              if (emptyCount > 1) {
                return ConformanceCheckResult.simple(Set.of(
                    "Expected at most one moves object without a carrierCode; found %s!"
                        .formatted(emptyCount)));
              }
              if (carrierCodes.stream().distinct().count() != carrierCodes.size()) {
                return ConformanceCheckResult.simple(Set.of(
                    "Expected carrierCodes to be different in the given moves objects; found multiple are the same!"));
              }
            }
            return ConformanceCheckResult.simple(Collections.emptySet());
          });

  static final JsonRebaseableContentCheck TIMESTAMP_ALLOWS_PORT_CALL_SERVICE_LOCATION =
      JsonAttribute.ifThen(
          "If timestamp is not of type REQ, it should not have a portCallServiceLocation",
          jsonNode ->
              !jsonNode.path(CLASSIFIER_CODE).asText("").equals(JitClassifierCode.REQ.name()),
          JsonAttribute.mustBeAbsent(JsonPointer.compile("/portCallServiceLocation")));

  static final JsonRebaseableContentCheck TIMESTAMP_VALIDATE_PORT_CALL_SERVICE_LOCATION =
      JsonAttribute.ifThen(
          "If timestamp has a portCallServiceLocation, it should have an UNLocationCode field.",
          jsonNode -> jsonNode.has("portCallServiceLocation"),
          JsonAttribute.mustBeNotNull(
              JsonPointer.compile("/portCallServiceLocation/UNLocationCode"),
              "it is a mandatory property of portCallServiceLocation."));

  public static final JsonRebaseableContentCheck
      VESSEL_NEEDS_ONE_OF_VESSEL_IMO_NUMBER_OR_MMSI_NUMBER =
          JsonAttribute.ifThen(
              "Vessel should at least have vesselIMONumber or MMSINumber",
              jsonNode -> jsonNode.has("vessel"),
              JsonAttribute.atLeastOneOfMatched(
                  (jsonNode, jsonPointers) -> {
                    jsonPointers.add(JsonPointer.compile("/vessel/vesselIMONumber"));
                    jsonPointers.add(JsonPointer.compile("/vessel/MMSINumber"));
                  }));

  public static final JsonRebaseableContentCheck
      VESSEL_WIDTH_OR_LENGTH_OVERALL_REQUIRES_DIMENSION_UNIT =
          JsonAttribute.ifThen(
              "Vessel: width or lengthOverall requires dimensionUnit property.",
              jsonNode ->
                  jsonNode.path("vessel").has("width")
                      || jsonNode.path("vessel").has("lengthOverall"),
              JsonAttribute.mustBePresent(JsonPointer.compile("/vessel/dimensionUnit")));

  public static final JsonRebaseableContentCheck VESSELSTATUS_DRAFTS_NEED_DIMENSION_UNIT =
      JsonAttribute.ifThen(
          "Property dimensionUnit is mandatory to provide if 'draft', 'airDraft', 'aftDraft' or 'forwardDraft' is provided.",
          jsonNode ->
              jsonNode.has("draft")
                  || jsonNode.has("airDraft")
                  || jsonNode.has("aftDraft")
                  || jsonNode.has("forwardDraft"),
          JsonAttribute.mustBePresent(JsonPointer.compile("/dimensionUnit")));

  static final JsonRebaseableContentCheck IS_FYI_TRUE =
      JsonAttribute.mustEqual(
          "Expected isFYI=true when message is For Your Information only.",
          JsonPointer.compile("/isFYI"),
          true);

  public static ActionCheck createChecksForPortCallService(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      PortCallServiceTypeCode serviceType,
      DynamicScenarioParameters dsp) {
    if (dsp == null) return null;
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(checkRightFieldValues());
    if (serviceType != null) {
      checks.add(checkPortCallService(serviceType));
    }
    if (dsp.selector() != JitServiceTypeSelector.GIVEN
        && dsp.selector() != JitServiceTypeSelector.ANY) {
      checks.add(checkPortCallServiceRightType(dsp));
    }
    if (dsp.portCallServiceTypeCode() == PortCallServiceTypeCode.MOVES) {
      checks.add(checkPortCallServiceHasMoves(true));
      checks.add(MOVES_CARRIER_CODE_IMPLIES_CARRIER_CODE_LIST_PROVIDER);
      checks.add(MOVES_CARRIER_CODE_LIST_PROVIDER_IMPLIES_CARRIER_CODE);
      checks.add(MOVES_OBJECTS_VERIFY_CARRIER_CODES);
    } else checks.add(checkPortCallServiceHasMoves(false));
    checks.add(JitChecks.checkCallIDMatchPreviousCallID(dsp));
    if (dsp.isFYI()) {
      checks.add(IS_FYI_TRUE);
    }
    return JsonAttribute.contentChecks(
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        checks);
  }

  static JsonContentCheck checkPortCallServiceHasMoves(boolean shouldHaveMoves) {
    if (shouldHaveMoves) {
      return JsonAttribute.mustBePresent(JsonPointer.compile("/moves"));
    }
    return JsonAttribute.mustBeAbsent(JsonPointer.compile("/moves"));
  }

  static final Predicate<JsonNode> IS_PORT_CALL_SERVICE = node -> node.has(PORT_CALL_SERVICE_TYPE);

  static JsonRebaseableContentCheck checkPortCallService(PortCallServiceTypeCode serviceType) {
    return JsonAttribute.ifThen(
        "Expected Port Call Service type should match scenario (%s).".formatted(serviceType.name()),
        IS_PORT_CALL_SERVICE,
        JsonAttribute.mustEqual(
            "Check if the correct Port Call Service was supplied.",
            JsonPointer.compile("/portCallServiceTypeCode"),
            serviceType::name));
  }

  static JsonContentCheck checkPortCallServiceRightType(DynamicScenarioParameters dsp) {
    return JsonAttribute.customValidator(
        "Port Call Service type should match scenario '%s'."
            .formatted(dsp.selector().getFullName()),
        body -> {
          if (IS_PORT_CALL_SERVICE.test(body)) {
            String actualServiceType = body.path(PORT_CALL_SERVICE_TYPE).asText();
            PortCallServiceTypeCode serviceType =
                PortCallServiceTypeCode.fromName(actualServiceType);
            if ((dsp.selector() == JitServiceTypeSelector.FULL_ERP
                    && !PortCallServiceTypeCode.getServicesWithERPAndA().contains(serviceType))
                || (dsp.selector() == JitServiceTypeSelector.S_A_PATTERN
                    && !PortCallServiceTypeCode.getServicesHavingOnlyA().contains(serviceType))) {
              return ConformanceCheckResult.simple(Set.of(
                  "Expected matching Port Call Service type with scenario '%s'. Found non-matching type: '%s'"
                      .formatted(dsp.selector().getFullName(), actualServiceType)));
            }
          }
          return ConformanceCheckResult.simple(Collections.emptySet());
        });
  }

  public static JsonContentCheck checkExpectedResultCount(
      int expectedResults, boolean moreResultsAllowed) {
    String orMore = moreResultsAllowed ? " or more" : "";
    return JsonAttribute.customValidator(
        "GET action should receive the right amount of results: %s%s."
            .formatted(expectedResults, orMore),
        body -> {
          if (!body.isArray()
              || (!moreResultsAllowed && body.size() != expectedResults)
              || (moreResultsAllowed && body.size() < expectedResults)) {
            return ConformanceCheckResult.simple(Set.of(
                "Expected %s%s result(s), but got %s result(s)."
                    .formatted(expectedResults, orMore, body.size())));
          }
          return ConformanceCheckResult.simple(Collections.emptySet());
        });
  }

  static JsonContentCheck checkRightFieldValues() {
    return JsonAttribute.customValidator(
        "Check if valid combinations of values are supplied.",
        body -> {
          Set<String> issues = new HashSet<>();
          if (IS_PORT_CALL_SERVICE.test(body)) {
            String actualServiceType = body.path(PORT_CALL_SERVICE_TYPE).asText();
            issues.add(verifyPortCallServiceEventTypeCode(body, actualServiceType));
          }
          return ConformanceCheckResult.simple(issues.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
        });
  }

  private static String verifyPortCallServiceEventTypeCode(
      JsonNode body, String actualServiceType) {
    String portCallServiceEventTypeCode = body.path("portCallServiceEventTypeCode").asText();
    if (!PortCallServiceEventTypeCode.isValidCombination(
        PortCallServiceTypeCode.fromName(actualServiceType), portCallServiceEventTypeCode)) {
      return "Expected matching Port Call Service type with PortCallServiceEventTypeCode. Found non-matching type: '%s' combined with code: '%s'"
          .formatted(actualServiceType, portCallServiceEventTypeCode);
    }
    return null;
  }

  public static List<JsonContentCheck> createChecksForTimestamp(DynamicScenarioParameters dsp) {
    List<JsonContentCheck> checks = new ArrayList<>();

    if (dsp.currentTimestamp() != null
        && dsp.currentTimestamp().classifierCode().equals(JitClassifierCode.PLN)) {
      checks.add(checkPlannedMatchesRequestedTimestamp(dsp));
    }
    if (dsp.previousTimestamp() == null) {
      checks.add(checkTimestampReplyTimestampIDisAbsent());
    }
    checks.add(checkCallIDMatchPreviousCallID(dsp));
    checks.add(checkTimestampIDsMatchesPreviousCall(dsp));
    checks.add(TIMESTAMP_ALLOWS_PORT_CALL_SERVICE_LOCATION);
    checks.add(TIMESTAMP_VALIDATE_PORT_CALL_SERVICE_LOCATION);
    return checks;
  }

  static JsonContentCheck checkTimestampIDsMatchesPreviousCall(DynamicScenarioParameters dsp) {
    return JsonAttribute.customValidator(
        "Check if the reply timestamp matches the previous timestamp.",
        body -> {
          if (dsp.previousTimestamp() == null) return ConformanceCheckResult.simple(Collections.emptySet());
          String previousTimestampID = dsp.previousTimestamp().timestampID();
          JitTimestamp timestamp = JitTimestamp.fromJson(body);
          if (!previousTimestampID.equals(timestamp.replyToTimestampID())) {
            return ConformanceCheckResult.simple(Set.of(
                "Expected replyToTimestampID matches previous sent timestampId: '%s' but found replyToTimestampID: '%s'"
                    .formatted(previousTimestampID, timestamp.replyToTimestampID())));
          }
          return ConformanceCheckResult.simple(Collections.emptySet());
        });
  }

  static JsonContentCheck checkTimestampReplyTimestampIDisAbsent() {
    return JsonAttribute.mustBeAbsent(JsonPointer.compile("/replyToTimestampID"));
  }

  static JsonContentCheck checkPlannedMatchesRequestedTimestamp(DynamicScenarioParameters dsp) {
    return JsonAttribute.customValidator(
        "Check if the Planned timestamp matches the Requested timestamp.",
        body -> {
          JitTimestamp receivedTimestamp = JitTimestamp.fromJson(body);
          if (dsp.previousTimestamp().classifierCode().equals(JitClassifierCode.REQ)
              && !dsp.previousTimestamp().dateTime().equals(receivedTimestamp.dateTime())) {
            return ConformanceCheckResult.simple(Set.of(
                "Expected matching timestamp: '%s' but got Planned timestamp: '%s'"
                    .formatted(dsp.previousTimestamp().dateTime(), receivedTimestamp.dateTime())));
          }
          return ConformanceCheckResult.simple(Collections.emptySet());
        });
  }

  public static JsonContentCheck checkCallIDMatchPreviousCallID(DynamicScenarioParameters dsp) {
    return JsonAttribute.customValidator(
        "Check if the used call ID matches the previous call' ID.",
        body -> {
          Set<String> errors = new HashSet<>();
          if (body.has(PORT_CALL_ID)
              && dsp.portCallID() != null
              && !body.path(PORT_CALL_ID).asText().equals(dsp.portCallID())) {
            errors.add(
                "Expected matching portCallID: '%s' but got a different portCallID: '%s'"
                    .formatted(dsp.portCallID(), body.path(PORT_CALL_ID)));
          }
          if (body.has(TERMINAL_CALL_ID)
              && dsp.terminalCallID() != null
              && !body.path(TERMINAL_CALL_ID).asText().equals(dsp.terminalCallID())) {
            errors.add(
                "Expected matching terminalCallID: '%s' but got a different terminalCallID: '%s'"
                    .formatted(dsp.terminalCallID(), body.path(TERMINAL_CALL_ID)));
          }
          if (body.has(PORT_CALL_SERVICE_ID)
              && dsp.portCallServiceID() != null
              && !body.path(PORT_CALL_SERVICE_ID).asText().equals(dsp.portCallServiceID())) {
            errors.add(
                "Expected matching portCallServiceID: '%s' but got a different portCallServiceID: '%s'"
                    .formatted(dsp.portCallServiceID(), body.path(PORT_CALL_SERVICE_ID)));
          }
          return ConformanceCheckResult.simple(errors);
        });
  }

  public static ActionCheck checkIsFYIIsCorrect(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      DynamicScenarioParameters dsp) {
    if (!dsp.isFYI()) return null;
    return JsonAttribute.contentChecks(
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        IS_FYI_TRUE);
  }
}
