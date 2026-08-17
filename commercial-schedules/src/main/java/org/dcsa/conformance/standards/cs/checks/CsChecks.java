package org.dcsa.conformance.standards.cs.checks;


import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.cs.party.CsRole;
import org.dcsa.conformance.standards.cs.party.DynamicScenarioParameters;

@UtilityClass
public class CsChecks {

  public static ActionCheck getPayloadChecksForPtp(
      UUID matchedExchangeUuid, String expectedApiVersion) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE_PTP);
    // checks.add(VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP);
    // checks.add(VALIDATE_CUTOFF_TIME_CODE);
    checks.add(VALIDATE_PTP_CUTOFF_TIMES_AT_ROUTING_OR_LEG_LEVEL);
    checks.add(VALIDATE_PTP_ROUTING_REFERENCE);
    checks.add(VALIDATE_PTP_SOLUTION_FOOTPRINT);
    checks.add(VALIDATE_PTP_LEG_FOOTPRINT);
    return JsonAttribute.contentChecks(
        CsRole::isProducer,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }

  static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE =
      JsonAttribute.customValidator(
          "Validate allowed cutoff codes",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();
            var index = new AtomicInteger(0);

            if (JsonUtil.isMissingOrEmpty(body)) {
              errors.add(ConformanceError.irrelevant(0));
              return ConformanceCheckResult.withRelevance(errors);
            }

            for (JsonNode routing : body) {
              int currentIndex = index.getAndIncrement();
              JsonNode cutOffTimes = routing.path("cutOffTimes");
              boolean routingHasCutOffTimes = !JsonUtil.isMissingOrEmpty(cutOffTimes);

              if (routingHasCutOffTimes) {
                cutOffTimes.forEach(
                    cutOffTime -> {
                      JsonNode cutOffDateTimeCode = cutOffTime.path("cutOffDateTimeCode");
                      if (!CsDataSets.CUTOFF_DATE_TIME_CODES.contains(
                          cutOffDateTimeCode.asText())) {
                        errors.add(
                            ConformanceError.error(
                                "Invalid cutOffDateTimeCode '%s' at routings[%d]"
                                    .formatted(cutOffDateTimeCode.asText(), currentIndex)));
                      }
                    });
              }

              // Also validate cutOffTimes codes at legs level
              JsonNode legs = routing.path("legs");
              if (!JsonUtil.isMissingOrEmpty(legs)) {
                var legIndex = new AtomicInteger(0);
                for (JsonNode leg : legs) {
                  int currentLegIndex = legIndex.getAndIncrement();
                  JsonNode legCutOffTimes = leg.path("cutOffTimes");
                  if (!JsonUtil.isMissingOrEmpty(legCutOffTimes)) {
                    legCutOffTimes.forEach(
                        cutOffTime -> {
                          JsonNode cutOffDateTimeCode = cutOffTime.path("cutOffDateTimeCode");
                          if (!CsDataSets.CUTOFF_DATE_TIME_CODES.contains(
                              cutOffDateTimeCode.asText())) {
                            errors.add(
                                ConformanceError.error(
                                    "Invalid cutOffDateTimeCode '%s' at routings[%d].legs[%d]"
                                        .formatted(
                                            cutOffDateTimeCode.asText(),
                                            currentIndex,
                                            currentLegIndex)));
                          }
                        });
                  }
                }
              }

              if (!routingHasCutOffTimes) {
                errors.add(ConformanceError.irrelevant(currentIndex));
              }
            }
            return ConformanceCheckResult.withRelevance(errors);
          });

  static final JsonContentCheck VALIDATE_PTP_CUTOFF_TIMES_AT_ROUTING_OR_LEG_LEVEL =
      JsonAttribute.customValidator(
          "Cut-off information: at least one routing must have cutOffTimes at routing or leg level",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();

            if (JsonUtil.isMissingOrEmpty(body)) {
              errors.add(ConformanceError.irrelevant());
              return ConformanceCheckResult.withRelevance(errors);
            }

            // fieldPresent = the cutOffTimes key exists somewhere (even if empty [])
            // hasNonEmpty  = at least one routing or leg has a non-empty cutOffTimes array
            boolean fieldPresent = false;
            boolean hasNonEmpty = false;

            outer:
            for (JsonNode routing : body) {
              JsonNode routingCutOff = routing.path("cutOffTimes");
              if (!routingCutOff.isMissingNode() && !routingCutOff.isNull()) {
                fieldPresent = true;
                if (routingCutOff.isArray() && !routingCutOff.isEmpty()) {
                  hasNonEmpty = true;
                  break;
                }
              }
              JsonNode legs = routing.path("legs");
              if (!JsonUtil.isMissingOrEmpty(legs)) {
                for (JsonNode leg : legs) {
                  JsonNode legCutOff = leg.path("cutOffTimes");
                  if (!legCutOff.isMissingNode() && !legCutOff.isNull()) {
                    fieldPresent = true;
                    if (legCutOff.isArray() && !legCutOff.isEmpty()) {
                      hasNonEmpty = true;
                      break outer;
                    }
                  }
                }
              }
            }

            if (!fieldPresent) {
              // cutOffTimes key never appears anywhere — not demonstrated
              errors.add(ConformanceError.irrelevant());
            } else if (!hasNonEmpty) {
              // key is present somewhere but every array is empty — fails the "non-empty"
              // requirement
              errors.add(
                  ConformanceError.error(
                      "cutOffTimes field is present but no routing or leg contains a non-empty cutOffTimes[] array."));
            }
            // else: at least one non-empty array found — passes
            return ConformanceCheckResult.withRelevance(errors);
          });

  // 4.1.3 — Booking routing reference (optional, report-only)
  static final JsonContentCheck VALIDATE_PTP_ROUTING_REFERENCE =
      JsonAttribute.customValidator(
          "Booking routing reference: at least one routing must have a non-empty routingReference",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();

            if (JsonUtil.isMissingOrEmpty(body)) {
              errors.add(ConformanceError.irrelevant());
              return ConformanceCheckResult.withRelevance(errors);
            }

            boolean fieldPresent = false;
            boolean anyNonEmpty = false;
            for (JsonNode routing : body) {
              JsonNode ref = routing.path("routingReference");
              if (!ref.isMissingNode() && !ref.isNull()) {
                fieldPresent = true;
              }
              if (ref.isTextual() && !ref.asText().isBlank()) {
                anyNonEmpty = true;
                break;
              }
            }

            if (!fieldPresent) {
              errors.add(ConformanceError.irrelevant());
            } else if (!anyNonEmpty) {
              errors.add(
                  ConformanceError.error(
                      "routingReference is present but no routing contains a non-empty routingReference value."));
            }
            return ConformanceCheckResult.withRelevance(errors);
          });

  // 4.1.3 — Footprint emissions per routing solution (optional, report-only)
  static final JsonContentCheck VALIDATE_PTP_SOLUTION_FOOTPRINT =
      JsonAttribute.customValidator(
          "Footprint emissions per routing solution: at least one routing must have solutionFootprint",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();

            if (JsonUtil.isMissingOrEmpty(body)) {
              errors.add(ConformanceError.irrelevant());
              return ConformanceCheckResult.withRelevance(errors);
            }

            boolean fieldPresent = false;
            boolean anyNonEmpty = false;
            for (JsonNode routing : body) {
              JsonNode fp = routing.path("solutionFootprint");
              if (!fp.isMissingNode() && !fp.isNull()) {
                fieldPresent = true;
              }
              if (!fp.isMissingNode() && !fp.isNull() && fp.isObject() && !fp.isEmpty()) {
                anyNonEmpty = true;
                break;
              }
            }

            if (!fieldPresent) {
              errors.add(ConformanceError.irrelevant());
            } else if (!anyNonEmpty) {
              errors.add(
                  ConformanceError.error(
                      "solutionFootprint is present but no routing contains a non-empty solutionFootprint object."));
            }
            return ConformanceCheckResult.withRelevance(errors);
          });

  // 4.1.3 — Footprint emissions per leg (optional, report-only)
  static final JsonContentCheck VALIDATE_PTP_LEG_FOOTPRINT =
      JsonAttribute.customValidator(
          "Footprint emissions per leg: at least one legs[] item must have footprint",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();

            if (JsonUtil.isMissingOrEmpty(body)) {
              errors.add(ConformanceError.irrelevant());
              return ConformanceCheckResult.withRelevance(errors);
            }

            boolean fieldPresent = false;
            boolean anyNonEmpty = false;
            outer:
            for (JsonNode routing : body) {
              JsonNode legs = routing.path("legs");
              if (!JsonUtil.isMissingOrEmpty(legs)) {
                for (JsonNode leg : legs) {
                  JsonNode fp = leg.path("footprint");
                  if (!fp.isMissingNode() && !fp.isNull()) {
                    fieldPresent = true;
                  }
                  if (!fp.isMissingNode() && !fp.isNull() && fp.isObject() && !fp.isEmpty()) {
                    anyNonEmpty = true;
                    break outer;
                  }
                }
              }
            }

            if (!fieldPresent) {
              errors.add(ConformanceError.irrelevant());
            } else if (!anyNonEmpty) {
              errors.add(
                  ConformanceError.error(
                      "footprint is present but no leg contains a non-empty footprint object."));
            }
            return ConformanceCheckResult.withRelevance(errors);
          });

  static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP =
      JsonAttribute.customValidator(
          "Validate 'cutOffDateTimeCode' and 'receiptTypeAtOrigin'",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();
            var index = new AtomicInteger(0);

            if (JsonUtil.isMissingOrEmpty(body)) {
              errors.add(ConformanceError.irrelevant(0));
              return ConformanceCheckResult.withRelevance(errors);
            }

            for (JsonNode routing : body) {
              int currentIndex = index.getAndIncrement();
              var receiptTypeAtOrigin = routing.path("receiptTypeAtOrigin").asText("");
              var shipmentCutOffTimes = routing.path("cutOffTimes");

              if ("CFS".equalsIgnoreCase(receiptTypeAtOrigin)) {
                errors.add(ConformanceError.irrelevant(currentIndex));
                continue;
              }

              if (JsonUtil.isMissingOrEmpty(shipmentCutOffTimes)) {
                errors.add(ConformanceError.irrelevant(currentIndex));
                continue;
              }

              var cutOffDateTimeCodes =
                  StreamSupport.stream(shipmentCutOffTimes.spliterator(), false)
                      .map(p -> p.path("cutOffDateTimeCode"))
                      .filter(JsonNode::isTextual)
                      .map(JsonNode::asText)
                      .collect(Collectors.toSet());

              if (!cutOffDateTimeCodes.contains("LCO")) {
                errors.add(ConformanceError.irrelevant(currentIndex));
                continue;
              }

              if (!"CFS".equalsIgnoreCase(receiptTypeAtOrigin)
                  && cutOffDateTimeCodes.contains("LCO")) {
                errors.add(
                    ConformanceError.error(
                        "cutOffDateTimeCode 'LCO' must not be present when receiptTypeAtOrigin is not 'CFS' "
                            + "(at routing index %d)".formatted(currentIndex)));
              }
            }

            return ConformanceCheckResult.withRelevance(errors);
          });

  static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE_PS =
      JsonAttribute.customValidator(
          "Validate allowed cutoff codes in vessel schedules",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();
            var index = new AtomicInteger(0);

            if (JsonUtil.isMissingOrEmpty(body)) {
              errors.add(ConformanceError.irrelevant(0));
              return ConformanceCheckResult.withRelevance(errors);
            }

            for (JsonNode schedule : body) {
              int currentIndex = index.getAndIncrement();
              var vesselSchedules = schedule.path("vesselSchedules");

              if (JsonUtil.isMissingOrEmpty(vesselSchedules)) {
                errors.add(ConformanceError.irrelevant(currentIndex));
                continue;
              }

              boolean hasCutOffTimes = false;

              for (JsonNode vesselSchedule : vesselSchedules) {
                var cutOffTimes = vesselSchedule.path("cutOffTimes");

                if (JsonUtil.isMissingOrEmpty(cutOffTimes)) {
                  continue;
                }

                hasCutOffTimes = true;

                cutOffTimes.forEach(
                    cutOffTime -> {
                      JsonNode cutOffDateTimeCode = cutOffTime.path("cutOffDateTimeCode");
                      if (!CsDataSets.CUTOFF_DATE_TIME_CODES.contains(
                          cutOffDateTimeCode.asText())) {
                        errors.add(
                            ConformanceError.error(
                                "Invalid cutOffDateTimeCode with value '%s' found at vesselSchedules[%d]"
                                    .formatted(cutOffDateTimeCode.asText(), currentIndex)));
                      }
                    });
              }

              if (!hasCutOffTimes) {
                errors.add(ConformanceError.irrelevant(currentIndex));
              }
            }

            return ConformanceCheckResult.withRelevance(errors);
          });

  static final JsonContentCheck VALIDATE_PS_VESSEL_SCHEDULES_EXISTS =
      JsonAttribute.customValidator(
          "At least one returned Port Schedule must contain vesselSchedules[] with at least one item",
          body -> {
            if (JsonUtil.isMissingOrEmpty(body)) {
              return ConformanceCheckResult.simple(
                  Set.of("At least one Port Schedule must be included in the response."));
            }

            for (JsonNode portSchedule : body) {
              JsonNode vesselSchedules = portSchedule.path("vesselSchedules");
              if (!JsonUtil.isMissingOrEmpty(vesselSchedules)) {
                return ConformanceCheckResult.simple(Set.of());
              }
            }

            return ConformanceCheckResult.simple(
                Set.of(
                    "At least one returned Port Schedule must contain vesselSchedules[] with at least one item."));
          });

  static final JsonContentCheck VALIDATE_PS_CUTOFF_INFORMATION_OPTIONAL =
      JsonAttribute.customValidator(
          "Cut-off information: at least one returned Port Schedule must contain a non-empty vesselSchedules[].cutOffTimes[] array",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();

            if (JsonUtil.isMissingOrEmpty(body)) {
              errors.add(ConformanceError.irrelevant());
              return ConformanceCheckResult.withRelevance(errors);
            }

            boolean hasAnyCutOffTimesField = false;
            boolean hasAnyNonEmptyCutOffTimes = false;

            for (JsonNode portSchedule : body) {
              JsonNode vesselSchedules = portSchedule.path("vesselSchedules");
              if (JsonUtil.isMissingOrEmpty(vesselSchedules)) {
                continue;
              }

              for (JsonNode vesselSchedule : vesselSchedules) {
                JsonNode cutOffTimes = vesselSchedule.path("cutOffTimes");
                if (!cutOffTimes.isMissingNode() && !cutOffTimes.isNull()) {
                  hasAnyCutOffTimesField = true;
                  if (cutOffTimes.isArray() && cutOffTimes.size() > 0) {
                    hasAnyNonEmptyCutOffTimes = true;
                    break;
                  }
                }
              }

              if (hasAnyNonEmptyCutOffTimes) {
                break;
              }
            }

            if (hasAnyNonEmptyCutOffTimes) {
              return ConformanceCheckResult.simple(Set.of());
            }

            if (!hasAnyCutOffTimesField) {
              errors.add(ConformanceError.irrelevant());
              return ConformanceCheckResult.withRelevance(errors);
            }

            errors.add(
                ConformanceError.error(
                    "At least one returned Port Schedule must contain a non-empty vesselSchedules[].cutOffTimes[] array."));
            return ConformanceCheckResult.withRelevance(errors);
          });

  static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE_VS =
      JsonAttribute.customValidator(
          "At least one Service Schedule must be included in the root response array.",
          body ->
              ConformanceCheckResult.simple(
                  body.isEmpty() ? Set.of("The response body must not be empty") : Set.of()));

  static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE_PS =
      JsonAttribute.customValidator(
          "At least one Port Schedule must be included in the root response array.",
          body ->
              ConformanceCheckResult.simple(
                  body.isEmpty() ? Set.of("The response body must not be empty") : Set.of()));

  static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE_PTP =
      JsonAttribute.customValidator(
          "At least one Point-to-Point Routing must be included in the root response array.",
          body ->
              ConformanceCheckResult.simple(
                  body.isEmpty() ? Set.of("The response body must not be empty") : Set.of()));

  private static JsonContentCheck paginationCheck(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.customValidator(
        "Check the response is paginated correctly",
        body -> {
          var issues = new LinkedHashSet<ConformanceError>();

          if (JsonUtil.isMissingOrEmpty(body)) {
            issues.add(ConformanceError.irrelevant());
            return ConformanceCheckResult.withRelevance(issues);
          }

          String firstPageHash = dspSupplier.get().firstPage();
          String secondPageHash = dspSupplier.get().secondPage();
          if (Objects.equals(firstPageHash, secondPageHash)) {
            ConformanceError.error("The second page must be different from the first page");
          }
          return ConformanceCheckResult.withRelevance(issues);
        });
  }

  public static ActionCheck getPayloadChecksForPs(
      UUID matchedExchangeUuid, String expectedApiVersion) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE_PS);
    checks.add(VALIDATE_PS_VESSEL_SCHEDULES_EXISTS);
    // checks.add(VALIDATE_CUTOFF_TIME_CODE_PS);
    checks.add(VALIDATE_PS_CUTOFF_INFORMATION_OPTIONAL);

    return JsonAttribute.contentChecks(
        CsRole::isProducer,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }

  static final JsonContentCheck VALIDATE_VS_VESSEL_SCHEDULES_EXISTS =
      JsonAttribute.customValidator(
          "At least one returned Service Schedule must contain vesselSchedules[] with at least one item",
          body -> {
            if (JsonUtil.isMissingOrEmpty(body)) {
              return ConformanceCheckResult.simple(
                  Set.of("At least one Service Schedule must be included in the response."));
            }
            for (JsonNode serviceSchedule : body) {
              if (!JsonUtil.isMissingOrEmpty(serviceSchedule.path("vesselSchedules"))) {
                return ConformanceCheckResult.simple(Set.of());
              }
            }
            return ConformanceCheckResult.simple(
                Set.of(
                    "At least one returned Service Schedule must contain vesselSchedules[] with at least one item."));
          });

  static final JsonContentCheck VALIDATE_VS_TRANSPORT_CALLS_EXISTS =
      JsonAttribute.customValidator(
          "At least one vesselSchedules[] item within at least one returned Service Schedule must contain transportCalls[] with at least one item",
          body -> {
            if (JsonUtil.isMissingOrEmpty(body)) {
              return ConformanceCheckResult.simple(
                  Set.of("At least one Service Schedule must be included in the response."));
            }
            for (JsonNode serviceSchedule : body) {
              JsonNode vesselSchedules = serviceSchedule.path("vesselSchedules");
              if (JsonUtil.isMissingOrEmpty(vesselSchedules)) continue;
              for (JsonNode vs : vesselSchedules) {
                if (!JsonUtil.isMissingOrEmpty(vs.path("transportCalls"))) {
                  return ConformanceCheckResult.simple(Set.of());
                }
              }
            }
            return ConformanceCheckResult.simple(
                Set.of(
                    "At least one vesselSchedules[] item must contain transportCalls[] with at least one item."));
          });

  static final JsonContentCheck VALIDATE_VS_TRANSPORT_CALL_LOCATION_EXISTS =
      JsonAttribute.customValidator(
          "At least one vesselSchedules[].transportCalls[] item must demonstrate the correct use of the location object",
          body -> {
            if (JsonUtil.isMissingOrEmpty(body)) {
              return ConformanceCheckResult.simple(
                  Set.of("At least one Service Schedule must be included in the response."));
            }
            for (JsonNode serviceSchedule : body) {
              JsonNode vesselSchedules = serviceSchedule.path("vesselSchedules");
              if (JsonUtil.isMissingOrEmpty(vesselSchedules)) continue;
              for (JsonNode vs : vesselSchedules) {
                JsonNode transportCalls = vs.path("transportCalls");
                if (JsonUtil.isMissingOrEmpty(transportCalls)) continue;
                for (JsonNode tc : transportCalls) {
                  JsonNode location = tc.path("location");
                  if (!location.isMissingNode() && !location.isNull() && location.isObject()) {
                    return ConformanceCheckResult.simple(Set.of());
                  }
                }
              }
            }
            return ConformanceCheckResult.simple(
                Set.of(
                    "At least one vesselSchedules[].transportCalls[] item must demonstrate the correct use of the location object."));
          });

  static final JsonContentCheck VALIDATE_VS_CUTOFF_INFORMATION_OPTIONAL =
      JsonAttribute.customValidator(
          "Cut-off information: at least one vesselSchedules[].transportCalls[].cutOffTimes[] must be non-empty",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();
            if (JsonUtil.isMissingOrEmpty(body)) {
              errors.add(ConformanceError.irrelevant());
              return ConformanceCheckResult.withRelevance(errors);
            }
            boolean hasField = false;
            boolean hasNonEmpty = false;
            outer:
            for (JsonNode serviceSchedule : body) {
              JsonNode vesselSchedules = serviceSchedule.path("vesselSchedules");
              if (JsonUtil.isMissingOrEmpty(vesselSchedules)) continue;
              for (JsonNode vs : vesselSchedules) {
                JsonNode transportCalls = vs.path("transportCalls");
                if (JsonUtil.isMissingOrEmpty(transportCalls)) continue;
                for (JsonNode tc : transportCalls) {
                  JsonNode cutOffTimes = tc.path("cutOffTimes");
                  if (!cutOffTimes.isMissingNode() && !cutOffTimes.isNull()) {
                    hasField = true;
                    if (cutOffTimes.isArray() && cutOffTimes.size() > 0) {
                      hasNonEmpty = true;
                      break outer;
                    }
                  }
                }
              }
            }
            if (hasNonEmpty) {
              return ConformanceCheckResult.simple(Set.of());
            }
            if (!hasField) {
              errors.add(ConformanceError.irrelevant());
              return ConformanceCheckResult.withRelevance(errors);
            }
            errors.add(
                ConformanceError.error(
                    "At least one vesselSchedules[].transportCalls[].cutOffTimes[] must be non-empty."));
            return ConformanceCheckResult.withRelevance(errors);
          });

  public static ActionCheck getPayloadChecksForVs(
      UUID matchedExchangeUuid, String expectedApiVersion) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE_VS);
    checks.add(VALIDATE_VS_VESSEL_SCHEDULES_EXISTS);
    checks.add(VALIDATE_VS_TRANSPORT_CALLS_EXISTS);
    checks.add(VALIDATE_VS_TRANSPORT_CALL_LOCATION_EXISTS);
    // Optional response-content validation is evaluated for all VS producer scenarios.
    // It reports irrelevant when not demonstrated, and fails only when present-but-invalid.
    checks.add(VALIDATE_VS_CUTOFF_INFORMATION_OPTIONAL);

    return JsonAttribute.contentChecks(
        CsRole::isProducer,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }
}
