package org.dcsa.conformance.standards.ovs.checks;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonContentMatchedValidation;
import org.dcsa.conformance.core.report.ConformanceStatus;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.ovs.party.OvsRole;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@UtilityClass
public class OvsChecks {

  private static final JsonContentMatchedValidation NON_EMPTY_VESSEL_SCHEDULES =
    JsonAttribute.path("vesselSchedules", JsonAttribute.matchedMinLength(1));

  private static final JsonContentMatchedValidation NON_EMPTY_TRANSPORT_CALLS =
    JsonAttribute.path("transportCalls", JsonAttribute.matchedMinLength(1));

  private static final JsonContentMatchedValidation NON_EMPTY_LOCATION =
    JsonAttribute.path("location", JsonAttribute.matchedMustBeNonEmpty());

  private static final JsonContentMatchedValidation NON_EMPTY_TIMESTAMPS =
    JsonAttribute.path("timestamps", JsonAttribute.matchedMinLength(1));

  private static final JsonContentMatchedValidation NON_EMPTY_UNIVERSAL_SERVICE_REFERENCE_IF_PRESENT =
    JsonAttribute.ifMatchedThen(
      node -> !JsonUtil.isMissing(node.path("universalServiceReference")),
      JsonAttribute.path(
        "universalServiceReference", JsonAttribute.matchedMustBeNonEmpty()));

  private static final JsonContentMatchedValidation NON_EMPTY_UNIVERSAL_VOYAGE_REFERENCE_IF_PRESENT =
    (transportCall, contextPath) -> {
      boolean importPresent =
        !JsonUtil.isMissing(transportCall.path("universalImportVoyageReference"));
      boolean exportPresent =
        !JsonUtil.isMissing(transportCall.path("universalExportVoyageReference"));

      if (!importPresent && !exportPresent) {
        return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
      }

      ConformanceCheckResult importResult =
        JsonAttribute.path(
            "universalImportVoyageReference", JsonAttribute.matchedMustBeNonEmpty())
          .validate(transportCall, contextPath);
      ConformanceCheckResult exportResult =
        JsonAttribute.path(
            "universalExportVoyageReference", JsonAttribute.matchedMustBeNonEmpty())
          .validate(transportCall, contextPath);

      if (importResult.isConformant() || exportResult.isConformant()) {
        return ConformanceCheckResult.simple(Set.of());
      }
      return ConformanceCheckResult.from(Set.of(importResult, exportResult));
    };

  private static final JsonContentMatchedValidation NON_EMPTY_STATUS_CODES_IF_PRESENT =
    JsonAttribute.ifMatchedThen(
      node -> !JsonUtil.isMissing(node.path("statusCodes")),
      JsonAttribute.path("statusCodes", JsonAttribute.matchedMinLength(1)));

  private static final JsonContentMatchedValidation DUMMY_VESSEL_NAME_WHEN_APPLICABLE =
    JsonAttribute.allMatched(
      "vesselSchedules",
      JsonAttribute.ifMatchedThen(
        JsonAttribute.isTrue("isDummyVessel"),
        JsonAttribute.path("vesselName", JsonAttribute.matchedMustBeNonEmpty())));

  public static ActionCheck mandatoryResponseContentChecks(UUID matched, String standardVersion) {
    return JsonAttribute.contentChecks(
      OvsRole::isPublisher, matched, HttpMessageType.RESPONSE, standardVersion,
      buildMandatoryResponseContentChecks());
  }

  public static ActionCheck optionalResponseContentChecks(UUID matched, String standardVersion) {
    ActionCheck check = JsonAttribute.contentChecks(
      "",
      "The HTTP response has valid content (optional response-content validations)",
      OvsRole::isPublisher,
      matched,
      HttpMessageType.RESPONSE,
      standardVersion,
      buildOptionalResponseContentChecks()
    );
    check.withStatusOverride(ConformanceStatus.PARTIALLY_CONFORMANT, false);
    return check;
  }

  private List<JsonContentCheck> buildMandatoryResponseContentChecks() {
    var checks = new ArrayList<JsonContentCheck>();

    checks.add(
      JsonAttribute.customValidator(
        "At least one service schedule must be included in the response.",
        body -> ConformanceCheckResult.simple(validateServiceSchedulesExist(body))));

    checks.add(
      JsonAttribute.customValidator(
        "At least one returned service schedule must contain a vesselSchedules array with at least one item.",
        body -> ConformanceCheckResult.simple(validateVesselSchedulesExist(body))));

    checks.add(
      JsonAttribute.customValidator(
        "At least one vesselSchedules[] item within at least one returned service schedule must contain a transportCalls array with at least one item.",
        body -> ConformanceCheckResult.simple(validateTransportCallsExist(body))));

    checks.add(
      JsonAttribute.customValidator(
        "At least one vesselSchedules[].transportCalls[] item within at least one returned service schedule must demonstrate the correct use of the location object.",
        body -> ConformanceCheckResult.simple(validateTransportCallLocationExists(body))));

    checks.add(
      JsonAttribute.customValidator(
        "At least one vesselSchedules[].transportCalls[] item within at least one returned service schedule must contain a timestamps array with at least one item.",
        body -> ConformanceCheckResult.simple(validateTransportCallTimestampsExist(body))));

    return checks;
  }

  private List<JsonContentCheck> buildOptionalResponseContentChecks() {
    var checks = new ArrayList<JsonContentCheck>();

    checks.add(
      JsonAttribute.customValidator(
        "At least one returned service schedule must demonstrate the correct use of universalServiceReference (not empty or blank).",
        OvsChecks::validateOptionalUniversalServiceReference));

    checks.add(
      JsonAttribute.customValidator(
        "At least one vesselSchedules[].transportCalls[] item within at least one returned service schedule must demonstrate the correct use of universalImportVoyageReference or universalExportVoyageReference (not empty or blank).",
        OvsChecks::validateOptionalUniversalVoyageReference));

    checks.add(
      JsonAttribute.customValidator(
        "At least one vesselSchedules[].transportCalls[] item within at least one returned service schedule must demonstrate the correct use of a statusCodes array with at least one item.",
        OvsChecks::validateOptionalStatusCodes));

    checks.add(
      JsonAttribute.customValidator(
        "Every vesselSchedules[] item with isDummyVessel set to true must demonstrate the correct use of vesselName (not empty or blank).",
        OvsChecks::validateOptionalDummyVesselName));

    return checks;
  }

  List<JsonContentCheck> buildResponseContentChecks() {
    var checks = new ArrayList<JsonContentCheck>();
    checks.addAll(buildMandatoryResponseContentChecks());
    checks.addAll(buildOptionalResponseContentChecks());
    return checks;
  }

  private Set<String> validateServiceSchedulesExist(JsonNode body) {
    if (body == null || body.isNull() || body.isMissingNode()) {
      return Set.of("Response body is missing or null.");
    }
    if (!body.isArray()) {
      return Set.of("Response must be an array of service schedules.");
    }
    if (body.isEmpty()) {
      return Set.of("Response must include at least one service schedule.");
    }
    return Set.of();
  }

  private Set<String> validateVesselSchedulesExist(JsonNode body) {
    var prerequisiteErrors = validateServiceSchedulesExist(body);
    if (!prerequisiteErrors.isEmpty()) {
      return prerequisiteErrors;
    }
    return requireAtLeastOneScheduleMatch(body, NON_EMPTY_VESSEL_SCHEDULES,
      "No returned service schedule contains a vesselSchedules array with at least one item.");
  }

  private Set<String> validateTransportCallsExist(JsonNode body) {
    var prerequisiteErrors = validateVesselSchedulesExist(body);
    if (!prerequisiteErrors.isEmpty()) {
      return prerequisiteErrors;
    }
    return requireAtLeastOneVesselScheduleMatch(body, NON_EMPTY_TRANSPORT_CALLS,
      "No vesselSchedules[] item within returned service schedules contains a transportCalls array with at least one item.");
  }

  private Set<String> validateTransportCallLocationExists(JsonNode body) {
    var prerequisiteErrors = validateTransportCallsExist(body);
    if (!prerequisiteErrors.isEmpty()) {
      return prerequisiteErrors;
    }
    return requireAtLeastOneTransportCallMatch(body, NON_EMPTY_LOCATION,
      "No vesselSchedules[].transportCalls[] item demonstrates a populated location object.");
  }

  private Set<String> validateTransportCallTimestampsExist(JsonNode body) {
    var prerequisiteErrors = validateTransportCallsExist(body);
    if (!prerequisiteErrors.isEmpty()) {
      return prerequisiteErrors;
    }
    return requireAtLeastOneTransportCallMatch(body, NON_EMPTY_TIMESTAMPS,
      "No vesselSchedules[].transportCalls[] item contains a timestamps array with at least one item.");
  }

  private ConformanceCheckResult validateOptionalUniversalServiceReference(JsonNode body) {
    var prerequisiteErrors = validateServiceSchedulesExist(body);
    if (!prerequisiteErrors.isEmpty()) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    return validateOptionalAtLeastOneScheduleMatch(body, NON_EMPTY_UNIVERSAL_SERVICE_REFERENCE_IF_PRESENT);
  }

  private ConformanceCheckResult validateOptionalUniversalVoyageReference(JsonNode body) {
    var prerequisiteErrors = validateTransportCallsExist(body);
    if (!prerequisiteErrors.isEmpty()) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    return validateOptionalAtLeastOneTransportCallMatch(body, NON_EMPTY_UNIVERSAL_VOYAGE_REFERENCE_IF_PRESENT);
  }

  private ConformanceCheckResult validateOptionalStatusCodes(JsonNode body) {
    var prerequisiteErrors = validateTransportCallsExist(body);
    if (!prerequisiteErrors.isEmpty()) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    return validateOptionalAtLeastOneTransportCallMatch(body, NON_EMPTY_STATUS_CODES_IF_PRESENT);
  }

  private ConformanceCheckResult validateOptionalDummyVesselName(JsonNode body) {
    var prerequisiteErrors = validateVesselSchedulesExist(body);
    if (!prerequisiteErrors.isEmpty()) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }

    Set<ConformanceCheckResult> scheduleResults = new LinkedHashSet<>();
    for (int scheduleIndex = 0; scheduleIndex < body.size(); scheduleIndex++) {
      scheduleResults.add(
        DUMMY_VESSEL_NAME_WHEN_APPLICABLE.validate(
          body.get(scheduleIndex), "response[%d]".formatted(scheduleIndex)));
    }

    if (scheduleResults.stream().noneMatch(ConformanceCheckResult::isRelevant)) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    return ConformanceCheckResult.from(scheduleResults);
  }

  private Set<String> requireAtLeastOneScheduleMatch(
    JsonNode body, JsonContentMatchedValidation validation, String failureMessage) {
    for (int scheduleIndex = 0; scheduleIndex < body.size(); scheduleIndex++) {
      if (validation
        .validate(body.get(scheduleIndex), "response[%d]".formatted(scheduleIndex))
        .isConformant()) {
        return Set.of();
      }
    }
    return Set.of(failureMessage);
  }

  private Set<String> requireAtLeastOneVesselScheduleMatch(
    JsonNode body, JsonContentMatchedValidation validation, String failureMessage) {
    for (int scheduleIndex = 0; scheduleIndex < body.size(); scheduleIndex++) {
      JsonNode vesselSchedules = body.get(scheduleIndex).path("vesselSchedules");
      if (!vesselSchedules.isArray()) {
        continue;
      }
      for (int vesselScheduleIndex = 0;
           vesselScheduleIndex < vesselSchedules.size();
           vesselScheduleIndex++) {
        if (validation
          .validate(
            vesselSchedules.get(vesselScheduleIndex),
            "response[%d].vesselSchedules[%d]"
              .formatted(scheduleIndex, vesselScheduleIndex))
          .isConformant()) {
          return Set.of();
        }
      }
    }
    return Set.of(failureMessage);
  }

  private Set<String> requireAtLeastOneTransportCallMatch(
    JsonNode body, JsonContentMatchedValidation validation, String failureMessage) {
    for (int scheduleIndex = 0; scheduleIndex < body.size(); scheduleIndex++) {
      JsonNode vesselSchedules = body.get(scheduleIndex).path("vesselSchedules");
      if (!vesselSchedules.isArray()) {
        continue;
      }
      for (int vesselScheduleIndex = 0;
           vesselScheduleIndex < vesselSchedules.size();
           vesselScheduleIndex++) {
        JsonNode transportCalls = vesselSchedules.get(vesselScheduleIndex).path("transportCalls");
        if (!transportCalls.isArray()) {
          continue;
        }
        for (int transportCallIndex = 0; transportCallIndex < transportCalls.size(); transportCallIndex++) {
          if (validation
            .validate(
              transportCalls.get(transportCallIndex),
              "response[%d].vesselSchedules[%d].transportCalls[%d]"
                .formatted(scheduleIndex, vesselScheduleIndex, transportCallIndex))
            .isConformant()) {
            return Set.of();
          }
        }
      }
    }
    return Set.of(failureMessage);
  }

  private ConformanceCheckResult validateOptionalAtLeastOneScheduleMatch(
    JsonNode body, JsonContentMatchedValidation validation) {
    Set<ConformanceCheckResult> scheduleResults = new LinkedHashSet<>();
    for (int scheduleIndex = 0; scheduleIndex < body.size(); scheduleIndex++) {
      ConformanceCheckResult result =
        validation.validate(body.get(scheduleIndex), "response[%d]".formatted(scheduleIndex));
      if (result.isRelevant() && result.isConformant()) {
        return ConformanceCheckResult.simple(Set.of());
      }
      scheduleResults.add(result);
    }
    if (scheduleResults.stream().noneMatch(ConformanceCheckResult::isRelevant)) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    return ConformanceCheckResult.from(scheduleResults);
  }

  private ConformanceCheckResult validateOptionalAtLeastOneTransportCallMatch(
    JsonNode body, JsonContentMatchedValidation validation) {
    Set<ConformanceCheckResult> transportCallResults = new LinkedHashSet<>();
    for (int scheduleIndex = 0; scheduleIndex < body.size(); scheduleIndex++) {
      JsonNode vesselSchedules = body.get(scheduleIndex).path("vesselSchedules");
      if (!vesselSchedules.isArray()) {
        continue;
      }
      for (int vesselScheduleIndex = 0;
           vesselScheduleIndex < vesselSchedules.size();
           vesselScheduleIndex++) {
        JsonNode transportCalls = vesselSchedules.get(vesselScheduleIndex).path("transportCalls");
        if (!transportCalls.isArray()) {
          continue;
        }
        for (int transportCallIndex = 0; transportCallIndex < transportCalls.size(); transportCallIndex++) {
          ConformanceCheckResult result =
            validation.validate(
              transportCalls.get(transportCallIndex),
              "response[%d].vesselSchedules[%d].transportCalls[%d]"
                .formatted(scheduleIndex, vesselScheduleIndex, transportCallIndex));
          if (result.isRelevant() && result.isConformant()) {
            return ConformanceCheckResult.simple(Set.of());
          }
          transportCallResults.add(result);
        }
      }
    }
    if (transportCallResults.stream().noneMatch(ConformanceCheckResult::isRelevant)) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    return ConformanceCheckResult.from(transportCallResults);
  }

}
