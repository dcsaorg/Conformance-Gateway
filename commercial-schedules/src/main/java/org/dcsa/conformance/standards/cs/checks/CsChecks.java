package org.dcsa.conformance.standards.cs.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.cs.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.cs.party.CsRole;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@UtilityClass
public class CsChecks {
  public static ActionCheck getPayloadChecksForPtp(UUID matchedExchangeUuid, String expectedApiVersion) {
    ArrayList<JsonContentCheck> checks = new ArrayList<JsonContentCheck>();
    checks.add(createLocationCheckforPtp("placeOfReceipt"));
    checks.add(createLocationCheckforPtp("placeOfDelivery"));
    checks.add(createLocationCheckforPtp("arrival"));
    checks.add(createLocationCheckforPtp("departure"));
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
              checkLocation(locationType, routings, issues);
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
        for (JsonNode schedules : body) {
          JsonNode address = schedules.at( "/location/address");
          JsonNode unLocationCode = schedules.at( "/location/UNLocationCode");
          JsonNode facility = schedules.at( "/location/facility");
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

  private static void checkLocation(String locationType, JsonNode routings, LinkedHashSet<String> issues) {
    String locationPath = String.format("/%s/location", locationType);
    JsonNode address = routings.at(locationPath + "/address");
    JsonNode unLocationCode = routings.at(locationPath + "/UNLocationCode");
    JsonNode facility = routings.at(locationPath + "/facility");
    if (JsonAttribute.isJsonNodeAbsent(address) && JsonAttribute.isJsonNodeAbsent(unLocationCode) && JsonAttribute.isJsonNodeAbsent(facility)) {
      issues.add(String.format("Any one of the location should be present for '%s'", locationType));
    }
  }


  private static final JsonContentCheck VALIDATE_CUTOFF_TIME_CODE = JsonAttribute.customValidator(
    "Validate shipment cutOff Date time code",
    (body) -> {
      var shipmentCutOffTimes = body.path("cutOffTimes");
      var receiptTypeAtOrigin = body.path("receiptTypeAtOrigin").asText("");
      var issues = new LinkedHashSet<String>();
      var cutOffDateTimeCodes = StreamSupport.stream(shipmentCutOffTimes.spliterator(), false)
        .map(p -> p.path("cutOffDateTimeCode"))
        .filter(JsonNode::isTextual)
        .map(n -> n.asText(""))
        .collect(Collectors.toSet());
      if (receiptTypeAtOrigin.equals("CFS") && !cutOffDateTimeCodes.contains("LCL")) {
        issues.add("cutOffDateTimeCode 'LCL' must be present when receiptTypeAtOrigin is CFS");
      }
      return issues;
    }
  );


/*  private static final JsonContentCheck CHECK_LOCATION_FIELDS_PLACE_OF_RECEIPT = JsonAttribute.customValidator(
    "Check any one of the location is available for location objects",
    body -> {
      var issues = new LinkedHashSet<String>();
      for(JsonNode routings : body){
        JsonNode address = routings.at("/placeOfReceipt/location/address");
        JsonNode unLocationCode = routings.at("/placeOfReceipt/location/UNLocationCode");
        JsonNode facility = routings.at("/placeOfReceipt/location/facility");
        if(JsonAttribute.isJsonNodeAbsent(address) && JsonAttribute.isJsonNodeAbsent(unLocationCode) && JsonAttribute.isJsonNodeAbsent(facility)) {
          issues.add("Any one of the location should be present for 'placeOfReceipt'");
        }
      }
      return issues;
    });*/

}
