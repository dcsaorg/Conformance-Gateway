package org.dcsa.conformance.standards.cs.checks;


import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
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
            var issues = new LinkedHashSet<String>();
            for (JsonNode routings : body) {
              routings
                  .at("/cutOffTimes")
                  .forEach(
                      cutOffTime -> {
                        JsonNode cutOffDateTimeCode = cutOffTime.at("/cutOffDateTimeCode");
                        if (!CsDataSets.CUTOFF_DATE_TIME_CODES.contains(
                            cutOffDateTimeCode.asText())) {
                          issues.add(
                              "Invalid cutOffDateTimeCode: %s"
                                  .formatted(cutOffDateTimeCode.asText()));
                        }
                      });
            }
            return issues;
          });

  static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN_PTP =
      JsonAttribute.customValidator(
          "Validate cutOff Date time code and receiptTypeAtOrigin",
          body -> {
            var issues = new LinkedHashSet<String>();
            for (JsonNode routing : body) {
              var shipmentCutOffTimes = routing.path("cutOffTimes");
              var receiptTypeAtOrigin = routing.path("receiptTypeAtOrigin").asText("");
              var cutOffDateTimeCodes =
                  StreamSupport.stream(shipmentCutOffTimes.spliterator(), false)
                      .map(p -> p.path("cutOffDateTimeCode"))
                      .filter(JsonNode::isTextual)
                      .map(n -> n.asText(""))
                      .collect(Collectors.toSet());
              if (!receiptTypeAtOrigin.equals("CFS") && cutOffDateTimeCodes.contains("LCO")) {
                issues.add(
                    "cutOffDateTimeCode 'LCO' must not be present when receiptTypeAtOrigin is not CFS");
              }
            }
            return issues;
          });
  static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE_PS =
      JsonAttribute.customValidator(
          "Validate allowed cutoff codes",
          body -> {
            var issues = new LinkedHashSet<String>();
            for (JsonNode schedule : body) {
              schedule
                  .at("/vesselSchedules")
                  .forEach(
                      vesselSchedule ->
                          vesselSchedule
                              .at("/cutOffTimes")
                              .forEach(
                                  cutOffTime -> {
                                    JsonNode cutOffDateTimeCode =
                                        cutOffTime.at("/cutOffDateTimeCode");
                                    if (!CsDataSets.CUTOFF_DATE_TIME_CODES.contains(
                                        cutOffDateTimeCode.asText())) {
                                      issues.add(
                                          "Invalid cutOffDateTimeCode: %s"
                                              .formatted(cutOffDateTimeCode.asText()));
                                    }
                                  }));
            }
            return issues;
          });

  static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE =
      JsonAttribute.customValidator(
          "Every response received during a conformance test must not be empty",
          body -> body.isEmpty() ? Set.of("The response body must not be empty") : Set.of());

  private static JsonContentCheck paginationCheck(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.customValidator(
        "Check the response is paginated correctly",
        body -> {
          var issues = new LinkedHashSet<String>();
          String firstPageHash = dspSupplier.get().firstPage();
          String secondPageHash = dspSupplier.get().secondPage();
          if (Objects.equals(firstPageHash, secondPageHash)) {
            issues.add("The second page must be different from the first page");
          }
          return issues;
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
