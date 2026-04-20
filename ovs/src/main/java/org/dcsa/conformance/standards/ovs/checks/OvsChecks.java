package org.dcsa.conformance.standards.ovs.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.ovs.party.OvsRole;

@Slf4j
@UtilityClass
public class OvsChecks {

  public List<JsonContentCheck> buildResponseContentChecks() {
    var checks = new ArrayList<JsonContentCheck>();

    checks.add(
        JsonAttribute.customValidator(
            "Every response received during a conformance test must contain schedules",
            body -> {
              Set<String> validationErrors = new LinkedHashSet<>();
              checkServiceSchedulesExist(body)
                  .forEach(
                      validationError ->
                          validationErrors.add(
                              "CheckServiceSchedules failed: %s".formatted(validationError)));
              return ConformanceCheckResult.simple(validationErrors);
            }));

    checks.add(
        JsonAttribute.customValidator(
            "Validate statusCodes in transport calls",
            body -> {
              Set<String> scheduleErrors = checkServiceSchedulesExist(body);
              if (!scheduleErrors.isEmpty()) {
                return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
              }

              return VALID_STATUS_CODES.validate(body);
            }));

    checks.add(
      JsonAttribute.customValidator(
        "Validate deprecated statusCode in transport calls",
        body -> {
          Set<String> scheduleErrors = checkServiceSchedulesExist(body);
          if (!scheduleErrors.isEmpty()) {
            return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
          }

          return VALID_DEPRECATED_STATUS_CODE.validate(body);
        }));

    return checks;
  }

  public static ActionCheck responseContentChecks(UUID matched, String standardVersion) {

    var checks = buildResponseContentChecks();
    return JsonAttribute.contentChecks(
        OvsRole::isPublisher, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  static final JsonContentCheck VALID_STATUS_CODES =
      JsonAttribute.customValidator(
          "Validate allowed status codes",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();
            int currentIndex = 0;
            boolean anyStatusCodeFound = false;

            for (JsonNode schedule : body) {
              for (JsonNode vesselSchedule : schedule.get("vesselSchedules")) {
                JsonNode transportCalls = vesselSchedule.get("transportCalls");
                if (JsonUtil.isMissingOrEmpty(transportCalls)) {
                  continue;
                }

                for (JsonNode transportCall : transportCalls) {
                  JsonNode statusCodes = transportCall.path("statusCodes");

                  if (!JsonUtil.isMissingOrEmpty(statusCodes)) {
                    anyStatusCodeFound = true;
                    for (JsonNode code : statusCodes) {
                      if (!OVSDataSets.STATUS_CODES.contains(code.asText())) {
                        errors.add(
                            ConformanceError.error(
                                "Invalid status '%s' in statusCodes at schedule [%d]"
                                    .formatted(code.asText(), currentIndex)));
                      }
                    }
                  }
                }
              }
              currentIndex++;
            }

            if (!anyStatusCodeFound) {
              errors.add(ConformanceError.irrelevant());
            }

            return ConformanceCheckResult.withRelevance(errors);
          });

  static final JsonContentCheck VALID_DEPRECATED_STATUS_CODE =
    JsonAttribute.customValidator(
      "Validate allowed deprecated statusCode",
      body -> {
        var errors = new LinkedHashSet<ConformanceError>();
        int currentIndex = 0;
        boolean anyStatusCodeFound = false;

        for (JsonNode schedule : body) {
          for (JsonNode vesselSchedule : schedule.get("vesselSchedules")) {
            JsonNode transportCalls = vesselSchedule.get("transportCalls");
            if (JsonUtil.isMissingOrEmpty(transportCalls)) {
              continue;
            }

            for (JsonNode transportCall : transportCalls) {
              JsonNode statusCode = transportCall.path("statusCode");

              // statusCodes (plural) takes precedence whenever the field is present — skip statusCode validation
              if (!JsonUtil.isMissing(transportCall.path("statusCodes"))) {
                continue;
              }

              if (!JsonUtil.isMissingOrEmpty(statusCode)) {
                anyStatusCodeFound = true;
                if (!OVSDataSets.STATUS_CODE.contains(statusCode.asText())) {
                  errors.add(
                    ConformanceError.error(
                      "Invalid deprecated statusCode '%s' at schedule [%d]"
                        .formatted(statusCode.asText(), currentIndex)));
                }
              }
            }
          }
          currentIndex++;
        }

        if (!anyStatusCodeFound) {
          errors.add(ConformanceError.irrelevant());
        }

        return ConformanceCheckResult.withRelevance(errors);
      });

  public Set<String> checkServiceSchedulesExist(JsonNode body) {

    if (body == null || body.isMissingNode() || body.isNull()) {
      return Set.of("Response body is missing or null.");
    }

    if (!body.isArray()) {
      return Set.of("Response must be an array of schedules.");
    }

    Set<String> errors = new LinkedHashSet<>();
    for (int i = 0; i < body.size(); i++) {
      JsonNode vesselSchedules = body.get(i).path("vesselSchedules");

      if (!vesselSchedules.isArray() || vesselSchedules.isEmpty()) {
        errors.add("Schedule at index %d does not contain vesselSchedules.".formatted(i));
      }

    }

    return errors.isEmpty() ? Set.of() : errors;
  }

}
