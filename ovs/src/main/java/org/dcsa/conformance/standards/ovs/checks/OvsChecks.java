package org.dcsa.conformance.standards.ovs.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
              Set<String> errors = new LinkedHashSet<>();
              Set<String> scheduleErrors = checkServiceSchedulesExist(body);
              if (!scheduleErrors.isEmpty()) {
                return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
              }

              var result = VALID_STATUS_CODE.validate(body);
              if (!result.getErrorMessages().isEmpty()) {
                errors.addAll(result.getErrorMessages());
              }
              return ConformanceCheckResult.simple(errors);
            }));

    return checks;
  }

  public static ActionCheck responseContentChecks(UUID matched, String standardVersion) {

    var checks = buildResponseContentChecks();
    return JsonAttribute.contentChecks(
        OvsRole::isPublisher, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  static final JsonContentCheck VALID_STATUS_CODE =
      JsonAttribute.customValidator(
          "Validate allowed status codes",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();

            var index = new AtomicInteger(0);

            if (JsonUtil.isMissingOrEmpty(body)) {
              errors.add(ConformanceError.irrelevant(0));
              return ConformanceCheckResult.withRelevance(errors);
            }

            for (JsonNode schedule : body) {
              int currentIndex = index.getAndIncrement();
              JsonNode vesselSchedules = schedule.get("vesselSchedules");

              if (JsonUtil.isMissingOrEmpty(vesselSchedules)) {
                errors.add(ConformanceError.irrelevant(currentIndex));
                continue;
              }

              for (JsonNode vesselSchedule : vesselSchedules) {
                JsonNode transportCalls = vesselSchedule.get("transportCalls");
                if (JsonUtil.isMissingOrEmpty(transportCalls)) {
                  errors.add(ConformanceError.irrelevant(currentIndex));
                  continue;
                }

                transportCalls.forEach(
                    transportCall -> {
                      JsonNode statusCode = transportCall.path("statusCode");
                      if (!OVSDataSets.STATUS_CODE.contains(statusCode.asText())) {
                        errors.add(
                            ConformanceError.error(
                                "Invalid status '%s' at [%d]"
                                    .formatted(statusCode.asText(), currentIndex)));
                      }
                    });
              }
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
    int index = 0;

    for (JsonNode schedule : body) {
      JsonNode vesselSchedules = schedule.path("vesselSchedules");

      if (!vesselSchedules.isArray() || vesselSchedules.isEmpty()) {
        errors.add("Schedule at index %d does not contain vesselSchedules.".formatted(index));
      }

      index++;
    }

    return errors.isEmpty() ? Set.of() : errors;
  }

}
