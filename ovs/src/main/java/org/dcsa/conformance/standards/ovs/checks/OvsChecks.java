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
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class OvsChecks {

  public static ActionCheck responseContentChecks(
      UUID matched, String standardVersion, Supplier<SuppliedScenarioParameters> sspSupplier) {

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
              if (validationErrors.isEmpty()) {
                return Set.of();
              }
              return validationErrors;
            }));

    checks.add(
        JsonAttribute.customValidator(
            "If present, at least schedule attribute must match the corresponding query parameters",
            body -> {
              Map<OvsFilterParameter, String> filterParametersMap = sspSupplier.get().getMap();
              Set<String> validationErrors = new LinkedHashSet<>();
              checkThatScheduleValuesMatchParamValues(body, filterParametersMap)
                  .forEach(
                      validationError ->
                          validationErrors.add(
                              "Schedule Param Value Validation failed: %s"
                                  .formatted(validationError)));
              return validationErrors;
            }));

    checks.add(
        JsonAttribute.customValidator(
            "Check eventDateTime is greater than or equal to startDate filter parameter if present",
            body -> {
              Set<String> validationErrors = new LinkedHashSet<>();
              Map<OvsFilterParameter, String> filterParametersMap = sspSupplier.get().getMap();
              validateDate(
                      body, filterParametersMap, OvsFilterParameter.START_DATE, LocalDate::isBefore)
                  .forEach(
                      validationError ->
                          validationErrors.add(
                              "Start Date EventDateTime validation failed: %s"
                                  .formatted(validationError)));
              return validationErrors;
            }));

    checks.add(
        JsonAttribute.customValidator(
            "Check eventDateTime is less than or equal to endDate filter parameter if present",
            body -> {
              Set<String> validationErrors = new LinkedHashSet<>();
              Map<OvsFilterParameter, String> filterParametersMap = sspSupplier.get().getMap();
              validateDate(
                      body, filterParametersMap, OvsFilterParameter.END_DATE, LocalDate::isAfter)
                  .forEach(
                      validationError ->
                          validationErrors.add(
                              "EndDate EventDateTime validation failed: %s"
                                  .formatted(validationError)));
              return validationErrors;
            }));

    checks.add(
        JsonAttribute.customValidator(
            "Check transportCallReference is unique across each service schedules",
            OvsChecks::validateUniqueTransportCallReference));

    checks.add(
        JsonAttribute.customValidator(
            "Validate limit exists and the number of schedules does not exceed the limit",
            body -> {
              Optional<Map.Entry<OvsFilterParameter, String>> limitParam =
                  sspSupplier.get().getMap().entrySet().stream()
                      .filter(e -> e.getKey().equals(OvsFilterParameter.LIMIT))
                      .findFirst();

              if (limitParam.isPresent()) {
                int expectedLimit = Integer.parseInt(limitParam.get().getValue().trim());
                if (body.size() > expectedLimit) {
                  return Set.of(
                      "The number of schedules exceeds the limit parameter: " + expectedLimit);
                }
              }
              return Set.of();
            }));

    return JsonAttribute.contentChecks(
        OvsRole::isPublisher, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  public Set<String> checkThatScheduleValuesMatchParamValues(
      JsonNode schedulesNode, Map<OvsFilterParameter, String> filterParametersMap) {
    Set<String> validationErrors = new LinkedHashSet<>();
    Arrays.stream(OvsFilterParameter.values())
        .filter(param -> !param.getJsonPaths().isEmpty())
        .filter(param -> !param.isSeparateCheckRequired())
        .filter(filterParametersMap::containsKey)
        .forEach(
            filterParameter -> {
              Set<String> parameterValues =
                  Arrays.stream(filterParametersMap.get(filterParameter).split(","))
                      .collect(Collectors.toSet());
              Set<String> attributeValues = new HashSet<>();
              Set<String> jsonPaths = filterParameter.getJsonPaths();
              jsonPaths.forEach(
                  jsonPathExpression -> {
                    findMatchingNodes(schedulesNode, jsonPathExpression)
                        .forEach(
                            node -> {
                              if (!node.isMissingNode() && !node.isNull()) {
                                attributeValues.add(node.asText());
                              }
                            });
                  });
              if (!attributeValues.isEmpty()
                  && parameterValues.stream().noneMatch(attributeValues::contains)) {
                validationErrors.add(
                    "Value%s '%s' at path%s '%s' do%s not match value%s of query parameter '%s'"
                        .formatted(
                            attributeValues.size() > 1 ? "s" : "",
                            String.join(", ", attributeValues),
                            String.join(", ", jsonPaths),
                            attributeValues.size() > 1 ? "" : "es",
                            filterParametersMap.get(filterParameter).contains(",") ? "s" : "",
                            String.join(", ", filterParametersMap.get(filterParameter).split(",")),
                            filterParameter.getQueryParamName()));
              }
            });

    return validationErrors;
  }

  public Set<String> validateDate(
      JsonNode body,
      Map<OvsFilterParameter, String> filterParametersMap,
      OvsFilterParameter dateParameter,
      BiPredicate<LocalDate, LocalDate> dateComparison) {

    Optional<Map.Entry<OvsFilterParameter, String>> dateParam =
        filterParametersMap.entrySet().stream()
            .filter(e -> e.getKey().equals(dateParameter))
            .findFirst();

    if (dateParam.isEmpty()) {
      return Set.of();
    }

    LocalDate expectedDate =
        LocalDate.parse(dateParam.get().getValue().trim(), DateTimeFormatter.ISO_DATE);

    Set<String> errors =
        findMatchingNodes(body, "*/vesselSchedules/*/transportCalls/*/timestamps")
            .flatMap(
                timestampsNode ->
                    StreamSupport.stream(timestampsNode.spliterator(), false)
                        .filter(
                            timestampNode -> {
                              LocalDate eventDateTime =
                                  stringToISODateTime(timestampNode.path("eventDateTime").asText());
                              if (eventDateTime != null) {
                                return dateComparison.test(eventDateTime, expectedDate);
                              }
                              return false;
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

  private static LocalDate stringToISODateTime(String dateTimeString) {
    try {
      return OffsetDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public Set<String> validateUniqueTransportCallReference(JsonNode body) {
    Set<String> transportCallReferences = new HashSet<>();
    Set<String> errors = new HashSet<>();
    // Iterate over each array node in the response body
    for (JsonNode node : body) {
      findMatchingNodes(node, "vesselSchedules/*/transportCalls/*/transportCallReference")
          .filter(tcrNode -> !tcrNode.isMissingNode() && !tcrNode.isNull())
          .forEach(
              transportCallReferenceNode -> {
                String transportCallReference = transportCallReferenceNode.asText();
                if (!transportCallReferences.add(transportCallReference)) {
                  errors.add("Duplicate transportCallReference found: " + transportCallReference);
                }
              });
    }
    return errors;
  }

  public Stream<JsonNode> findMatchingNodes(JsonNode node, String jsonPath) {
    if (jsonPath.isEmpty() || jsonPath.equals("/")) {
      return Stream.of(node);
    }
    String[] pathSegments = jsonPath.split("/");
    if (pathSegments[0].equals("*")) {
      if (node.isArray()) {
        // If the node is an array, iterate over its elements
        return StreamSupport.stream(node.spliterator(), false)
            .flatMap(
                childNode ->
                    findMatchingNodes(
                        childNode,
                        String.join(
                            "/", Arrays.copyOfRange(pathSegments, 1, pathSegments.length))));
      } else {
        // If not an array, treat it as a single node
        return findMatchingNodes(
            node, String.join("/", Arrays.copyOfRange(pathSegments, 1, pathSegments.length)));
      }
    } else {
      return findMatchingNodes(
          node.path(pathSegments[0]),
          String.join("/", Arrays.copyOfRange(pathSegments, 1, pathSegments.length)));
    }
  }

  public Set<String> checkServiceSchedulesExist(JsonNode body) {
    Set<String> validationErrors = new LinkedHashSet<>();
    if (body == null || body.isMissingNode() || body.isNull()) {
      validationErrors.add("Response body is missing or null.");
    } else {
      boolean hasVesselSchedules =
          findMatchingNodes(body, "*/vesselSchedules")
              .anyMatch(node -> !node.isMissingNode() && node.isArray() && !node.isEmpty());
      if (!hasVesselSchedules) {
        validationErrors.add("Response doesn't have schedules.");
      }
    }
    return validationErrors;
  }
}
