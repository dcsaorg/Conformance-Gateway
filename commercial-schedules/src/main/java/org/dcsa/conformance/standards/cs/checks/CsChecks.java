package org.dcsa.conformance.standards.cs.checks;

import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.cs.party.CsRole;
import org.dcsa.conformance.standards.cs.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.cs.party.SuppliedScenarioParameters;

@UtilityClass
public class CsChecks {
  private static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE =
      JsonAttribute.customValidator(
          "Validate shipment cutOff Date time code",
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
              if (receiptTypeAtOrigin.equals("CFS") && !cutOffDateTimeCodes.contains("LCL")) {
                issues.add(
                    "cutOffDateTimeCode 'LCL' must be present when receiptTypeAtOrigin is CFS");
              }
            }
            return issues;
          });
  private static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE_PS =
      JsonAttribute.customValidator(
          "Validate allowed shipment cutoff codes",
          body -> {
            var issues = new LinkedHashSet<String>();
            for (JsonNode schedule : body) {
              schedule
                  .at("/vesselSchedules")
                  .forEach(
                      vesselSchedule -> {
                        vesselSchedule
                            .at("/cutOffTimes")
                            .forEach(
                                cutOffTime -> {
                                  JsonNode cutOffDateTimeCode =
                                      cutOffTime.at("/cutOffDateTimeCode");
                                  if (!CsDataSets.CUTOFF_DATE_TIME_CODES.contains(
                                      cutOffDateTimeCode.asText())) {
                                    issues.add(
                                        String.format(
                                            "Invalid cutOffDateTimeCode: %s",
                                            cutOffDateTimeCode.asText()));
                                  }
                                });
                      });
            }
            return issues;
          });

  public static ActionCheck getPayloadChecksForPtp(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<SuppliedScenarioParameters> sspSupplier, Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(createLocationCheckPtp("placeOfReceipt"));
    checks.add(createLocationCheckPtp("placeOfDelivery"));
    checks.add(createLocationCheckPtp("arrival"));
    checks.add(createLocationCheckPtp("departure"));
    checks.add(VALIDATE_CUTOFF_TIME_CODE);
    if (sspSupplier.get() != null) {
      if (sspSupplier.get().getMap().containsKey(ARRIVAL_START_DATE)
          || sspSupplier.get().getMap().containsKey(ARRIVAL_END_DATE)
          || sspSupplier.get().getMap().containsKey(DEPARTURE_START_DATE)
          || sspSupplier.get().getMap().containsKey(DEPARTURE_END_DATE)) {
        checks.add(validateDateRangeforPtp(sspSupplier));
      }
      checks.add(paginationCheckForPtp(dspSupplier));
    }
    return JsonAttribute.contentChecks(
        CsRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }

  private static JsonContentCheck paginationCheckForPtp(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.customValidator(
      String.format("Check the response is paginated correctly"),
        body -> {
        JsonNode cursor = dspSupplier.get().toJson();
          var issues = new LinkedHashSet<String>();
          return issues;
        });

  }

  private static JsonContentCheck createLocationCheckPtp(String locationType) {
    return JsonAttribute.customValidator(
        String.format("Check any one of the location is available for '%s'", locationType),
        body -> {
          var issues = new LinkedHashSet<String>();
          for (JsonNode routing : body) {
            if (locationType.equals("arrival") || locationType.equals("departure")) {
              for (JsonNode leg : routing.path("legs")) {
                checkAnyLocationIsPresent(locationType, leg, issues);
              }
            } else {
              checkAnyLocationIsPresent(locationType, routing, issues);
            }
          }
          return issues;
        });
  }

  public static ActionCheck getPayloadChecksForPs(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<SuppliedScenarioParameters> sspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(createLocationCheckPs());
    checks.add(VALIDATE_CUTOFF_TIME_CODE_PS);
    checks.add(validateDateForPs(sspSupplier));
    return JsonAttribute.contentChecks(
        CsRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }

  private static JsonContentCheck createLocationCheckPs() {
    return JsonAttribute.customValidator(
        "Check any one of the location is available for location",
        body -> {
          var issues = new LinkedHashSet<String>();
          for (JsonNode schedule : body) {
            JsonNode address = schedule.at("/location/address");
            JsonNode unLocationCode = schedule.at("/location/UNLocationCode");
            JsonNode facility = schedule.at("/location/facility");
            if (JsonAttribute.isJsonNodeAbsent(address)
                && JsonAttribute.isJsonNodeAbsent(unLocationCode)
                && JsonAttribute.isJsonNodeAbsent(facility)) {
              issues.add("Any one of the location should be present for location");
            }
          }
          return issues;
        });
  }

  public static ActionCheck getPayloadChecksForVs(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<SuppliedScenarioParameters> sspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(createLocationCheckVs());
    if (sspSupplier.get() != null) {
      if (sspSupplier.get().getMap().containsKey(UNIVERSAL_SERVICE_REFERENCE)) {
        checks.add(validateUSRForVs(sspSupplier));
      }
      if (sspSupplier.get().getMap().containsKey(VESSEL_IMO_NUMBER)) {
        checks.add(validateIMONumberForVS(sspSupplier));
      }
    }
    return JsonAttribute.contentChecks(
        CsRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }

  private static JsonContentCheck createLocationCheckVs() {
    return JsonAttribute.customValidator(
        "Check any one of the location is available for location",
        body -> {
          return StreamSupport.stream(body.spliterator(), false)
              .flatMap(
                  schedules ->
                      StreamSupport.stream(schedules.at("/vesselSchedules").spliterator(), false))
              .flatMap(vs -> StreamSupport.stream(vs.at("/transportCalls").spliterator(), false))
              .map(tc -> tc.at("/location"))
              .filter(
                  location -> {
                    JsonNode address = location.at("/address");
                    JsonNode unLocationCode = location.at("/UNLocationCode");
                    JsonNode facility = location.at("/facility");
                    return JsonAttribute.isJsonNodeAbsent(address)
                        && JsonAttribute.isJsonNodeAbsent(unLocationCode)
                        && JsonAttribute.isJsonNodeAbsent(facility);
                  })
              .map(location -> "Any one of the location should be present for location")
              .collect(Collectors.toCollection(LinkedHashSet::new));
        });
  }

  private static void checkAnyLocationIsPresent(
      String locationType, JsonNode data, LinkedHashSet<String> issues) {
    String locationPath = String.format("/%s/location", locationType);
    JsonNode address = data.at(locationPath + "/address");
    JsonNode unLocationCode = data.at(locationPath + "/UNLocationCode");
    JsonNode facility = data.at(locationPath + "/facility");
    if (JsonAttribute.isJsonNodeAbsent(address)
        && JsonAttribute.isJsonNodeAbsent(unLocationCode)
        && JsonAttribute.isJsonNodeAbsent(facility)) {
      issues.add(String.format("Any one of the location should be present for '%s'", locationType));
    }
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
    if (dateType.equals("startDate") && !dateTimeAsDate.isAfter(date)) {
      return String.format("The %s date should be after the start date", operation);
    } else if (dateType.equals("endDate") && !dateTimeAsDate.isBefore(date)) {
      return String.format("The %s date should be before the end date", operation);
    }
    return "";
  }

  private static JsonContentCheck validateUSRForVs(
      Supplier<SuppliedScenarioParameters> sspSupplier) {
    return JsonAttribute.customValidator(
        "Validate USR available in the response",
        body -> {
          var issues = new LinkedHashSet<String>();
          for (JsonNode vs : body) {
            var csp = sspSupplier.get().getMap();
            if (csp.containsKey(UNIVERSAL_SERVICE_REFERENCE)) {
              if (vs.at("/universalServiceReference").isMissingNode()) {
                issues.add(
                    "The 'universalServiceReference' needs to be provided in the response if it is given in the filter.");
              }
            }
          }
          return issues;
        });
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
                    vesselSchedule -> {
                      vesselSchedule
                          .at("/timestamps")
                          .forEach(
                              timestamp -> {
                                JsonNode eventDateTime = timestamp.at("/eventDateTime");
                                JsonNode eventClassifierCode = timestamp.at("/eventClassifierCode");
                                if (eventClassifierCode.asText().equals("EST")
                                    || eventClassifierCode.asText().equals("PLN")) {
                                  if (isBeforeTheDate(
                                      eventDateTime.asText(),
                                      sspSupplier.get().getMap().get(DATE))) {
                                    issues.add(
                                        "The estimated arrival or departure dates should be on or after the date provided");
                                  }
                                }
                              });
                    });
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

  private JsonContentCheck validateIMONumberForVS(
      Supplier<SuppliedScenarioParameters> sspSupplier) {
    return JsonAttribute.customValidator(
        "Validate vesselIMONumber present in response",
        body -> {
          var issues = new LinkedHashSet<String>();
          for (JsonNode schedule : body) {
            for (JsonNode vs : schedule.at("/vesselSchedules")) {
              if (sspSupplier.get().getMap().containsKey(VESSEL_IMO_NUMBER)
                  && (vs.at("/vessel/vesselIMONumber").isMissingNode()
                      || vs.at("/vessel/vesselIMONumber").isNull())) {
                issues.add(
                    "VesselIMONumber should be present in the response if provided in the filter.");
              }
            }
          }
          return issues;
        });
  }
}
