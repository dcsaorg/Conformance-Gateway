package org.dcsa.conformance.standards.ovs.checks;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ovs.party.OvsFilterParameter;
import org.dcsa.conformance.standards.ovs.party.OvsRole;
import org.dcsa.conformance.standards.ovs.party.SuppliedScenarioParameters;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class OvsChecks {

  public static ActionCheck responseContentChecks(UUID matched, String standardVersion,
                                                  Supplier<SuppliedScenarioParameters> sspSupplier) {

    var checks = new ArrayList<JsonContentCheck>();

    checks.add(JsonAttribute.customValidator(
      "Validate carrierServiceCode exists and " +
        "matches in JSON response if request parameter has carrierServiceCode",
      body -> validateParameter(
        body,
        sspSupplier,
        OvsFilterParameter.CARRIER_SERVICE_CODE,
        "*/carrierServiceCode")
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate universalServiceReference exists and " +
        "matches in JSON response if request parameter has universalServiceReference",
      body -> validateParameter(
        body,
        sspSupplier,
        OvsFilterParameter.UNIVERSAL_SERVICE_REFERENCE,
        "*/universalServiceReference")
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate vesselIMONumber exists and" +
        " matches in JSON response if request parameter has vesselIMONumber",
      body -> validateParameter(
        body,
        sspSupplier,
        OvsFilterParameter.VESSEL_IMO_NUMBER,
        "*/vesselSchedules/*/vesselIMONumber")
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate vesselName exists and" +
        " matches in JSON response if request parameter has vesselName",
      body -> validateParameter(
        body,
        sspSupplier,
        OvsFilterParameter.VESSEL_NAME,
        "*/vesselSchedules/*/vesselName")
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate carrierVoyageNumber exists and " +
        "matches in JSON response if request parameter has carrierVoyageNumber",
      body -> validateParameter(
        body,
        sspSupplier,
        OvsFilterParameter.CARRIER_VOYAGE_NUMBER,
        "*/vesselSchedules/*/transportCalls/*/carrierExportVoyageNumber",
        "*/vesselSchedules/*/transportCalls/*/carrierImportVoyageNumber")
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate universalVoyageReference exists and" +
        " matches in JSON response if request parameter has universalVoyageReference",
      body -> validateParameter(
        body,
        sspSupplier,
        OvsFilterParameter.UNIVERSAL_VOYAGE_REFERENCE,
        "*/vesselSchedules/*/transportCalls/*/universalImportVoyageReference",
        "*/vesselSchedules/*/transportCalls/*/universalExportVoyageReference")
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate UNLocationCode exists and " +
        "matches in JSON response if request parameter has UNLocationCode",
      body -> validateParameter(
        body,
        sspSupplier,
        OvsFilterParameter.UN_LOCATION_CODE,
        "*/vesselSchedules/*/transportCalls/*/location/UNLocationCode")
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate facilitySMDGCode exists and " +
        "matches in JSON response if request parameter has facilitySMDGCode",
      body -> validateParameter(
        body,
        sspSupplier,
        OvsFilterParameter.FACILITY_SMDG_CODE,
        "*/vesselSchedules/*/transportCalls/*/location/facilitySMDGCode")
    ));

    checks.add(
      JsonAttribute.customValidator(
        "Validate startDate exists and " +
          "matches in JSON response if request parameter has startDate",
        body ->
          validateDate(
            body,
            sspSupplier,
            OvsFilterParameter.START_DATE,
            (eventDate, expectedStartDate) ->
              !eventDate.isAfter(expectedStartDate)))); // Check if eventDate is before or equal to startDate

    checks.add(
      JsonAttribute.customValidator(
        "Validate endDate exists and " +
          "matches in JSON response if request parameter has endDate",
        body ->
          validateDate(
            body,
            sspSupplier,
            OvsFilterParameter.END_DATE,
            (eventDate, expectedEndDate) ->
              !eventDate.isBefore(expectedEndDate)))); // Check if eventDate is after or equal to endDate

    checks.add(JsonAttribute.customValidator(
      "Validate transportCallReference is unique across each array node",
      OvsChecks::validateUniqueTransportCallReference
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate limit parameter is met",
      body -> {
        Optional<Map.Entry<OvsFilterParameter, String>> limitParam =
          sspSupplier.get().getMap().entrySet().stream()
            .filter(e -> e.getKey().equals(OvsFilterParameter.LIMIT))
            .findFirst();

        if (limitParam.isPresent()) {
          int expectedLimit = Integer.parseInt(limitParam.get().getValue().trim());
          if (body.size() > expectedLimit) {
            return Set.of("The number of events exceeds the limit parameter: " + expectedLimit);
          }
        }

        return Set.of();
      }
    ));

    return JsonAttribute.contentChecks(
      OvsRole::isPublisher,
      matched,
      HttpMessageType.RESPONSE,
      standardVersion,
      checks
    );
  }

  private Set<String> validateParameter(JsonNode body, Supplier<SuppliedScenarioParameters> sspSupplier,
                                        OvsFilterParameter parameter, String... jsonPaths) {
    Optional<Map.Entry<OvsFilterParameter, String>> param = sspSupplier.get().getMap().entrySet().stream()
      .filter(e -> e.getKey().equals(parameter))
      .findFirst();

    if (param.isPresent()) {
      Set<String> expectedValues = Arrays.stream(param.get().getValue().split(","))
        .map(String::trim)
        .collect(Collectors.toSet());

      // Check if ANY of the jsonPaths match the expectedValues
      if(jsonPaths.length > 1) {
        boolean anyMatch = Stream.of(jsonPaths)
          .anyMatch(jsonPath ->
            findMatchingNodes(body, jsonPath)
              .anyMatch(valueNode -> !(valueNode.isMissingNode() || valueNode.isNull())
                && expectedValues.contains(valueNode.asText())));

        if (!anyMatch) {
          return Set.of("Missing or mismatched values for parameter: " + parameter.getQueryParamName()
            + " in any of the paths: " + Arrays.toString(jsonPaths));
        }
      }
      else {
        Set<String> errors = Stream.of(jsonPaths)
          .flatMap(jsonPath ->
            findMatchingNodes(body, jsonPath)
              .filter(valueNode -> !(valueNode.isMissingNode() || valueNode.isNull())
                && !expectedValues.contains(valueNode.asText()))
              .map(valueNode -> "Missing or mismatched " + jsonPath + ": " + valueNode.asText()))
          .collect(Collectors.toSet());

        return errors.isEmpty() ? Set.of() : errors;
      }
    }
    return Set.of();
  }

  private Set<String> validateDate(
    JsonNode body,
    Supplier<SuppliedScenarioParameters> sspSupplier,
    OvsFilterParameter dateParameter,
    BiPredicate<LocalDate, LocalDate> dateComparison) {

    Optional<Map.Entry<OvsFilterParameter, String>> dateParam =
      sspSupplier.get().getMap().entrySet().stream()
        .filter(e -> e.getKey().equals(dateParameter))
        .findFirst();

    if (dateParam.isPresent()) {
      LocalDate expectedDate =
        LocalDate.parse(dateParam.get().getValue().trim(), DateTimeFormatter.ISO_DATE);

      Set<String> errors =
        findMatchingNodes(body, "*/vesselSchedules/*/transportCalls/*/timestamps")
          .flatMap(
            timestampsNode ->
              StreamSupport.stream(timestampsNode.spliterator(), false)
                .filter(
                  timestampNode -> {
                    OffsetDateTime eventDateTime =
                      OffsetDateTime.parse(
                        timestampNode.path("eventDateTime").asText(),
                        DateTimeFormatter.ISO_DATE_TIME);
                    return dateComparison.test(
                      eventDateTime.toLocalDate(), expectedDate);
                  }))
          .map(
            timestampNode ->
              "Event DateTime "
                + timestampNode.path("eventDateTime").asText()
                + (dateParameter == OvsFilterParameter.START_DATE
                ? " is before or equal to the startDate: "
                : " is after or equal to the endDate: ")
                + expectedDate)
          .collect(Collectors.toSet());

      return errors.isEmpty() ? Set.of() : errors;
    }

    return Set.of();
  }

  private Set<String> validateUniqueTransportCallReference(JsonNode body) {
    Set<String> transportCallReferences = new HashSet<>();
    Set<String> errors = new HashSet<>();

    // Iterate over each array node in the response body
    for (JsonNode node : body) {
      // Assuming the path to transportCallReference is consistent across array nodes
      findMatchingNodes(node, "*/vesselSchedules/*/transportCalls/*/transportCallReference")
        .forEach(transportCallReferenceNode -> {
          String transportCallReference = transportCallReferenceNode.asText();
          if (!transportCallReferences.add(transportCallReference)) {
            errors.add("Duplicate transportCallReference found: " + transportCallReference);
          }
        });
    }

    return errors;
  }

  private Stream<JsonNode> findMatchingNodes(JsonNode node, String jsonPath) {
    if (jsonPath.isEmpty() || jsonPath.equals("/")) {
      return Stream.of(node);
    }

    String[] pathSegments = jsonPath.split("/");
    if (pathSegments[0].equals("*")) {
      if (node.isArray()) {
        // If the node is an array, iterate over its elements
        return StreamSupport.stream(node.spliterator(), false)
          .flatMap(childNode ->
            findMatchingNodes(childNode,
              String.join("/",
                Arrays.copyOfRange(pathSegments, 1, pathSegments.length))));
      } else {
        // If not an array, treat it as a single node
        return findMatchingNodes(node,
          String.join("/",
            Arrays.copyOfRange(pathSegments, 1, pathSegments.length)));
      }
    } else {
      return findMatchingNodes(node.path(pathSegments[0]),
        String.join("/",
          Arrays.copyOfRange(pathSegments, 1, pathSegments.length)));
    }
  }

}
