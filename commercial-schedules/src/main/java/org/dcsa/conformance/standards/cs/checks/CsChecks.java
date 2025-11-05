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
import org.dcsa.conformance.standards.cs.party.CsRole;
import org.dcsa.conformance.standards.cs.party.DynamicScenarioParameters;

@UtilityClass
public class CsChecks {

  public static ActionCheck getPayloadChecksForPtp(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dspSupplier,
      boolean checkPagination) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    checks.add(VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP);
    checks.add(VALIDATE_CUTOFF_TIME_CODE);
    if (checkPagination && dspSupplier != null) {
      checks.add(paginationCheck(dspSupplier));
    }
    return JsonAttribute.contentChecks(
        CsRole::isPublisher,
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
            var routings = body;

            var index = new AtomicInteger(0);

            if (body == null || body.isMissingNode() || body.isEmpty()) {
              errors.add(ConformanceError.irrelevant(0));
              return ConformanceCheckResult.withRelevance(errors);
            }

            for (JsonNode routing : routings) {
              int currentIndex = index.getAndIncrement();
              JsonNode cutOffTimes = routing.path("cutOffTimes");

              if (cutOffTimes.isMissingNode() || cutOffTimes.isEmpty()) {
                errors.add(ConformanceError.irrelevant(currentIndex));
                continue;
              }

              cutOffTimes.forEach(
                  cutOffTime -> {
                    JsonNode cutOffDateTimeCode = cutOffTime.path("cutOffDateTimeCode");
                    if (!CsDataSets.CUTOFF_DATE_TIME_CODES.contains(cutOffDateTimeCode.asText())) {
                      errors.add(
                          ConformanceError.error(
                              "Invalid cutOffDateTimeCode '%s' at routings[%d]"
                                  .formatted(cutOffDateTimeCode.asText(), currentIndex)));
                    }
                  });
            }
            return ConformanceCheckResult.withRelevance(errors);
          });

  static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP =
      JsonAttribute.customValidator(
          "Validate cutOffDateTimeCode and receiptTypeAtOrigin",
          body -> {
            var errors = new LinkedHashSet<ConformanceError>();
            var index = new AtomicInteger(0);

            if (body == null || body.isMissingNode() || body.isEmpty()) {
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

              if (shipmentCutOffTimes.isMissingNode() || shipmentCutOffTimes.isEmpty()) {
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

            if (body == null || body.isMissingNode() || body.isEmpty()) {
              errors.add(ConformanceError.irrelevant(0));
              return ConformanceCheckResult.withRelevance(errors);
            }

            for (JsonNode schedule : body) {
              int currentIndex = index.getAndIncrement();
              var vesselSchedules = schedule.path("vesselSchedules");

              if (vesselSchedules.isMissingNode() || vesselSchedules.isEmpty()) {
                errors.add(ConformanceError.irrelevant(currentIndex));
                continue;
              }

              boolean hasCutOffTimes = false;

              for (JsonNode vesselSchedule : vesselSchedules) {
                var cutOffTimes = vesselSchedule.path("cutOffTimes");

                if (cutOffTimes.isMissingNode() || cutOffTimes.isEmpty()) {
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
                                "Invalid cutOffDateTimeCode '%s' found at vesselSchedules[%d]"
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

  static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE =
      JsonAttribute.customValidator(
          "Every response received during a conformance test must not be empty",
          body ->
              ConformanceCheckResult.simple(
                  body.isEmpty() ? Set.of("The response body must not be empty") : Set.of()));

  private static JsonContentCheck paginationCheck(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.customValidator(
        "Check the response is paginated correctly",
        body -> {
          var issues = new LinkedHashSet<ConformanceError>();

          if (body == null || body.isMissingNode() || body.isEmpty()) {
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
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dspSupplier,
      boolean checkPagination) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    checks.add(VALIDATE_CUTOFF_TIME_CODE_PS);
    if (checkPagination && dspSupplier != null) {
      checks.add(paginationCheck(dspSupplier));
    }
    return JsonAttribute.contentChecks(
        CsRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }

  public static ActionCheck getPayloadChecksForVs(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dspSupplier,
      boolean checkPagination) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    if (checkPagination && dspSupplier != null) {
      checks = new ArrayList<>();
      checks.add(paginationCheck(dspSupplier));
    }
    return JsonAttribute.contentChecks(
        CsRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }
}
