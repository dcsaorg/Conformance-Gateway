package org.dcsa.conformance.standards.cs.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.cs.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.cs.party.CsRole;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.*;

@UtilityClass
public class CsChecks {
  public static ActionCheck getPayloadChecksForPtp(UUID matchedExchangeUuid, String expectedApiVersion,Supplier<SuppliedScenarioParameters> sspSupplier) {
    ArrayList<JsonContentCheck> checks = new ArrayList<JsonContentCheck>();
    checks.add(createLocationCheckforPtp("placeOfReceipt"));
    checks.add(createLocationCheckforPtp("placeOfDelivery"));
    checks.add(createLocationCheckforPtp("arrival"));
    checks.add(createLocationCheckforPtp("departure"));
    checks.add(VALIDATE_CUTOFF_TIME_CODE);
    if(sspSupplier.get().getMap().containsKey(ARRIVAL_START_DATE)|| sspSupplier.get().getMap().containsKey(ARRIVAL_END_DATE)
      || sspSupplier.get().getMap().containsKey(DEPARTURE_START_DATE)|| sspSupplier.get().getMap().containsKey(DEPARTURE_END_DATE)) {
      checks.add(validateDateRange(sspSupplier));
    }
    return JsonAttribute.contentChecks(
      CsRole::isPublisher,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE,
      expectedApiVersion,
      checks
    );
  }

  private static JsonContentCheck createLocationCheckforPtp(String locationType) {
    return JsonAttribute.customValidator(
      String.format("Check any one of the location is available for '%s'", locationType),
      body -> {
        var issues = new LinkedHashSet<String>();
        for (JsonNode routings : body) {
          if (locationType.equals("arrival") || locationType.equals("departure")) {
            for(JsonNode leg : routings.path("legs")){
              checkLocation(locationType, leg, issues);
            }
          } else {
            checkLocation(locationType, routings, issues);
          }
        }
        return issues;
      }
    );
  }

  public static ActionCheck getPayloadChecksForPs(UUID matchedExchangeUuid, String expectedApiVersion) {
    ArrayList<JsonContentCheck> checks = new ArrayList<JsonContentCheck>();
    checks.add(createLocationCheckforPs());
    checks.add(VALIDATE_CUTOFF_TIME_CODE_PS);
    return JsonAttribute.contentChecks(
      CsRole::isPublisher,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE,
      expectedApiVersion,
      checks
    );
  }

  private static JsonContentCheck createLocationCheckforPs() {
    return JsonAttribute.customValidator("Check any one of the location is available for location",
      body -> {
        var issues = new LinkedHashSet<String>();
        for (JsonNode schedule : body) {
          JsonNode address = schedule.at( "/location/address");
          JsonNode unLocationCode = schedule.at( "/location/UNLocationCode");
          JsonNode facility = schedule.at( "/location/facility");
          if (JsonAttribute.isJsonNodeAbsent(address) && JsonAttribute.isJsonNodeAbsent(unLocationCode) && JsonAttribute.isJsonNodeAbsent(facility)) {
            issues.add("Any one of the location should be present for location");
          }
        }
        return issues;
      }
    );
  }

  public static ActionCheck getPayloadChecksForVs(UUID matchedExchangeUuid, String expectedApiVersion) {
    ArrayList<JsonContentCheck> checks = new ArrayList<JsonContentCheck>();
    checks.add(createLocationCheckforVs());
    return JsonAttribute.contentChecks(
      CsRole::isPublisher,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE,
      expectedApiVersion,
      checks
    );
  }

  private static JsonContentCheck createLocationCheckforVs() {
    return JsonAttribute.customValidator("Check any one of the location is available for location",
      body -> {
        var issues = StreamSupport.stream(body.spliterator(), false)
          .flatMap(schedules -> StreamSupport.stream(schedules.at("/vesselSchedules").spliterator(), false))
          .flatMap(vs -> StreamSupport.stream(vs.at("/transportCalls").spliterator(), false))
          .map(tc -> tc.at("/location"))
          .filter(location -> {
            JsonNode address = location.at("/address");
            JsonNode unLocationCode = location.at("/UNLocationCode");
            JsonNode facility = location.at("/facility");
            return JsonAttribute.isJsonNodeAbsent(address) &&
              JsonAttribute.isJsonNodeAbsent(unLocationCode) &&
              JsonAttribute.isJsonNodeAbsent(facility);
          })
          .map(location -> "Any one of the location should be present for location")
          .collect(Collectors.toCollection(LinkedHashSet::new));
        return issues;
      }
    );
  }

  private static void checkLocation(String locationType, JsonNode data, LinkedHashSet<String> issues) {
    String locationPath = String.format("/%s/location", locationType);
    JsonNode address = data.at(locationPath + "/address");
    JsonNode unLocationCode = data.at(locationPath + "/UNLocationCode");
    JsonNode facility = data.at(locationPath + "/facility");
    if (JsonAttribute.isJsonNodeAbsent(address) && JsonAttribute.isJsonNodeAbsent(unLocationCode) && JsonAttribute.isJsonNodeAbsent(facility)) {
      issues.add(String.format("Any one of the location should be present for '%s'", locationType));
    }
  }


  private static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE = JsonAttribute.customValidator(
    "Validate shipment cutOff Date time code",
    (body) -> {
      var issues = new LinkedHashSet<String>();
      for (JsonNode routing : body) {
        var shipmentCutOffTimes = routing.path("cutOffTimes");
        var receiptTypeAtOrigin = routing.path("receiptTypeAtOrigin").asText("");
        var cutOffDateTimeCodes = StreamSupport.stream(shipmentCutOffTimes.spliterator(), false)
          .map(p -> p.path("cutOffDateTimeCode"))
          .filter(JsonNode::isTextual)
          .map(n -> n.asText(""))
          .collect(Collectors.toSet());
        if (receiptTypeAtOrigin.equals("CFS") && !cutOffDateTimeCodes.contains("LCL")) {
          issues.add("cutOffDateTimeCode 'LCL' must be present when receiptTypeAtOrigin is CFS");
        }
      }
      return issues;
    }
  );


  private static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE_PS = JsonAttribute.customValidator(
    "Validate allowed shipment cutoff codes",
    body -> {
      var issues = new LinkedHashSet<String>();
      for (JsonNode schedule : body) {
        schedule.at("/vesselSchedules").forEach(vesselSchedule -> {
          vesselSchedule.at("/cutOffTimes").forEach(cutOffTime -> {
            JsonNode cutOffDateTimeCode = cutOffTime.at("/cutOffDateTimeCode");
            if (!CsDataSets.CUTOFF_DATE_TIME_CODES.contains(cutOffDateTimeCode.asText())) {
              issues.add(String.format("Invalid cutOffDateTimeCode: %s", cutOffDateTimeCode.asText()));
            }
          });
        });
      }
      return issues;
    }
  );

  private static final JsonContentCheck validateDateRange(Supplier<SuppliedScenarioParameters> sspSupplier){
      return JsonAttribute.customValidator(
        "Validate the dates for routings",
        body -> {
          var issues = new LinkedHashSet<String>();
          for (JsonNode schedule : body) {
            schedule.at("/legs").forEach(leg -> {
              JsonNode arrivalDateTime = leg.at("/arrival/dateTime");
              JsonNode departureDateTime = leg.at("/departure/dateTime");
              var csp = sspSupplier.get().getMap();

              if (!StringUtils.isEmpty(csp.get(ARRIVAL_START_DATE))) {
                issues.add(compareDates(arrivalDateTime.asText(), csp.get(ARRIVAL_START_DATE), "startDate", "arrival"));
              }
              if (!StringUtils.isEmpty(csp.get(ARRIVAL_END_DATE))) {
                issues.add(compareDates(arrivalDateTime.asText(), csp.get(ARRIVAL_END_DATE), "endDate", "arrival"));
              }
              if (!StringUtils.isEmpty(csp.get(DEPARTURE_START_DATE))) {
                issues.add(compareDates(departureDateTime.asText(), csp.get(DEPARTURE_START_DATE), "startDate", "departure"));
              }
              if (!StringUtils.isEmpty(csp.get(DEPARTURE_END_DATE))) {
                issues.add(compareDates(departureDateTime.asText(), csp.get(DEPARTURE_END_DATE), "endDate", "departure"));
              }
            });
          }
          return issues;
        }
      );
  }

  private String compareDates(String dateValue, String dateQueryParam, String dateType, String operation){
    LocalDate date = LocalDate.parse(dateQueryParam);
    ZonedDateTime dateTime = ZonedDateTime.parse(dateValue);
    LocalDate dateTimeAsDate = dateTime.toLocalDate();

    // Compare the dates
    if (dateType.equals("startDate")&& !dateTimeAsDate.isAfter(date)) {
      return String.format("The %s date should be after the %s start date",operation,operation);
    } else if (dateType.equals("endDate") && !dateTimeAsDate.isBefore(date)) {
      return String.format("The %s date should be before the %s start date",operation, operation);
    }
    return "";
  }

}
