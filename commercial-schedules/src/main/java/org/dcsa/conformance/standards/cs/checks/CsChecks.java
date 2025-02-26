package org.dcsa.conformance.standards.cs.checks;

import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonRebaseableContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.cs.party.CsRole;
import org.dcsa.conformance.standards.cs.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.cs.party.SuppliedScenarioParameters;

@UtilityClass
public class CsChecks {

  public static ActionCheck getPayloadChecksForPtp(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<SuppliedScenarioParameters> sspSupplier,
      Supplier<DynamicScenarioParameters> dspSupplier,
      boolean checkPagination) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN);
    checks.add(VALIDATE_CUTOFF_TIME_CODE);
    if (sspSupplier.get() != null) {
      if (sspSupplier.get().getMap().containsKey(ARRIVAL_START_DATE)
          || sspSupplier.get().getMap().containsKey(ARRIVAL_END_DATE)
          || sspSupplier.get().getMap().containsKey(DEPARTURE_START_DATE)
          || sspSupplier.get().getMap().containsKey(DEPARTURE_END_DATE)) {
        checks.add(validateDateRangeforPtp(sspSupplier));
      }
    }
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

  private static final JsonRebaseableContentCheck VALIDATE_CUTOFF_TIME_CODE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All 'cutOffDateTimeCode' fields must be valid",
          mav -> mav.submitAllMatching("cutOffTimes.*.cutOffDateTimeCode"),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(CsDataSets.CUTOFF_DATE_TIME_CODES));

  private static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE_AND_RECEIPTTYPEATORIGIN =
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
                    "cutOffDateTimeCode 'LCL' must not be present when receiptTypeAtOrigin is not CFS");
              }
            }
            return issues;
          });
  private static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE_PS =
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
      Supplier<SuppliedScenarioParameters> sspSupplier,
      Supplier<DynamicScenarioParameters> dspSupplier,
      boolean checkPagination) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_CUTOFF_TIME_CODE_PS);
    checks.add(validateDateForPs(sspSupplier));
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
      Supplier<SuppliedScenarioParameters> sspSupplier,
      Supplier<DynamicScenarioParameters> dspSupplier,
      boolean checkPagination) {
    var checks = new ArrayList<JsonContentCheck>();
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


  private static JsonContentCheck validateDateRangeforPtp(
      Supplier<SuppliedScenarioParameters> sspSupplier) {
    return JsonAttribute.customValidator(
        "Validate the dates for point to point routing",
        body -> {
          var issues = new LinkedHashSet<String>();
          for (JsonNode schedule : body) {
            schedule
                .at("/legs")
                .forEach(
                    leg -> {
                      JsonNode arrivalDateTime = leg.at("/arrival/dateTime");
                      JsonNode departureDateTime = leg.at("/departure/dateTime");
                      var csp = sspSupplier.get().getMap();

                      if (!StringUtils.isEmpty(csp.get(ARRIVAL_START_DATE))) {
                        String dateComparisonResult =
                            compareDates(
                                arrivalDateTime.asText(),
                                csp.get(ARRIVAL_START_DATE),
                                "startDate",
                                "arrival");
                        addIfNotBlank(dateComparisonResult, issues);
                      }
                      if (!StringUtils.isEmpty(csp.get(ARRIVAL_END_DATE))) {
                        String dateComparisonResult =
                            compareDates(
                                arrivalDateTime.asText(),
                                csp.get(ARRIVAL_END_DATE),
                                "endDate",
                                "arrival");
                        addIfNotBlank(dateComparisonResult, issues);
                      }
                      if (!StringUtils.isEmpty(csp.get(DEPARTURE_START_DATE))) {
                        String dateComparisonResult =
                            compareDates(
                                departureDateTime.asText(),
                                csp.get(DEPARTURE_START_DATE),
                                "startDate",
                                "departure");
                        addIfNotBlank(dateComparisonResult, issues);
                      }
                      if (!StringUtils.isEmpty(csp.get(DEPARTURE_END_DATE))) {
                        String dateComparisonResult =
                            compareDates(
                                departureDateTime.asText(),
                                csp.get(DEPARTURE_END_DATE),
                                "endDate",
                                "departure");
                        addIfNotBlank(dateComparisonResult, issues);
                      }
                    });
          }
          return issues;
        });
  }

  private void addIfNotBlank(String result, LinkedHashSet<String> issues) {
    if (!result.isBlank()) {
      issues.add(result);
    }
  }

  private String compareDates(
      String dateValue, String dateQueryParam, String dateType, String operation) {
    LocalDate date = LocalDate.parse(dateQueryParam);
    ZonedDateTime dateTime = ZonedDateTime.parse(dateValue);
    LocalDate dateTimeAsDate = dateTime.toLocalDate();

    // Compare the dates
    if (dateType.equals("startDate") && dateTimeAsDate.isBefore(date)) {
      return "The %s date should be after the start date".formatted(operation);
    } else if (dateType.equals("endDate") && dateTimeAsDate.isAfter(date)) {
      return "The %s date should be before the end date".formatted(operation);
    }
    return "";
  }

  private static JsonContentCheck validateDateForPs(
      Supplier<SuppliedScenarioParameters> sspSupplier) {
    return JsonAttribute.customValidator(
        "Validate date in the response",
        body -> {
          var issues = new LinkedHashSet<String>();
          for (JsonNode schedule : body) {
            schedule
                .at("/vesselSchedules")
                .forEach(
                    vesselSchedule ->
                        vesselSchedule
                            .at("/timestamps")
                            .forEach(
                                timestamp -> {
                                  JsonNode eventDateTime = timestamp.at("/eventDateTime");
                                  JsonNode eventClassifierCode =
                                      timestamp.at("/eventClassifierCode");
                                  if (eventClassifierCode.asText().equals("EST")
                                      || eventClassifierCode.asText().equals("PLN")) {
                                    if (isBeforeTheDate(
                                        eventDateTime.asText(),
                                        sspSupplier.get().getMap().get(DATE))) {
                                      issues.add(
                                          "The estimated arrival or departure dates should be on or after the date provided");
                                    }
                                  }
                                }));
          }
          return issues;
        });
  }

  private boolean isBeforeTheDate(String dateValue, String dateQueryParam) {
    LocalDate filterDate = LocalDate.parse(dateQueryParam);
    ZonedDateTime dateTime = ZonedDateTime.parse(dateValue);
    LocalDate responseDateTimeAsDate = dateTime.toLocalDate();
    return !responseDateTimeAsDate.isAfter(filterDate);
  }
}
