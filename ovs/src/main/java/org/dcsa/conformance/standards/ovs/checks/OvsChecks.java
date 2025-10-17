package org.dcsa.conformance.standards.ovs.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ovs.party.OvsFilterParameter;
import org.dcsa.conformance.standards.ovs.party.OvsRole;
import org.dcsa.conformance.standards.ovs.party.SuppliedScenarioParameters;

@Slf4j
@UtilityClass
public class OvsChecks {

  public List<JsonContentCheck> buildResponseContentChecks(Map<OvsFilterParameter, String> filterParametersMap) {
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
        "If present, at least one schedule attribute must match the corresponding query parameters",
        body -> {
          Set<String> validationErrors = new LinkedHashSet<>();
          checkThatScheduleValuesMatchParamValues(body, filterParametersMap)
            .forEach(
              validationError ->
                validationErrors.add(
                  "Schedule Param Value Validation failed: %s"
                    .formatted(validationError)));
          return ConformanceCheckResult.simple(validationErrors);
        }));

    checks.add(
      JsonAttribute.customValidator(
        "Check transportCallReference is unique across each service schedules",
              body -> ConformanceCheckResult.simple(validateUniqueTransportCallReference(body))));

    checks.add(
      JsonAttribute.customValidator(
        "Validate limit exists and the number of schedules does not exceed the limit",
        body -> {
          Optional<Map.Entry<OvsFilterParameter, String>> limitParam =
            filterParametersMap.entrySet().stream()
              .filter(e -> e.getKey().equals(OvsFilterParameter.LIMIT))
              .findFirst();

          if (limitParam.isPresent()) {
            int expectedLimit = Integer.parseInt(limitParam.get().getValue().trim());
            if (body.size() > expectedLimit) {
              return ConformanceCheckResult.simple(Set.of(
                "The number of service schedules exceeds the limit parameter: "
                  + expectedLimit));
            }
          }
          return ConformanceCheckResult.simple(Set.of());
        }));
    return checks;
  }

  public static ActionCheck responseContentChecks(
      UUID matched, String standardVersion, Supplier<SuppliedScenarioParameters> sspSupplier) {
    Map<OvsFilterParameter, String> filterParametersMap = sspSupplier.get() != null
      ? sspSupplier.get().getMap()
      : Map.of();
    var checks = buildResponseContentChecks(filterParametersMap);
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
              Set<Map.Entry<String, JsonNode>> attributeValues = new HashSet<>();
              Set<String> jsonPaths = filterParameter.getJsonPaths();
              jsonPaths.forEach(
                  jsonPathExpression -> {
                    findMatchingNodes(schedulesNode, jsonPathExpression)
                        .forEach(
                            result -> {
                              if (!result.getValue().isMissingNode()
                                  && !result.getValue().isNull()) {
                                attributeValues.add(result);
                              }
                            });
                  });
              if (!attributeValues.isEmpty()
                  && parameterValues.stream()
                      .noneMatch(
                          parameterValue ->
                              attributeValues.stream()
                                  .anyMatch(
                                      entry ->
                                          entry
                                              .getValue()
                                              .asText()
                                              .trim()
                                              .equals(
                                                  parameterValue.trim())))) { // Trim and compare

                String errorMessage =
                    String.format(
                        "Value%s '%s' at path%s '%s' do%s not match value%s '%s' of query parameter '%s'",
                        attributeValues.size() > 1 ? "s" : "",
                        attributeValues.stream()
                            .map(e -> e.getValue().asText())
                            .collect(Collectors.joining(", ")),
                        attributeValues.size() > 1 ? "s" : "",
                        attributeValues.stream()
                            .map(Map.Entry::getKey)
                            .collect(Collectors.joining(", ")), // Get keys here
                        attributeValues.size() > 1 ? "" : "es",
                        parameterValues.size() > 1 ? "s" : "",
                        String.join(", ", parameterValues),
                        filterParameter.getQueryParamName());

                validationErrors.add(errorMessage);
              }
            });

    return validationErrors;
  }


  public Set<String> validateUniqueTransportCallReference(JsonNode body) {
    Set<String> errors = new HashSet<>();
    // Iterate over each service schedule in the response body
    for (JsonNode node : body) {
      Set<String> transportCallReferences = new HashSet<>();
      findMatchingNodes(node, "vesselSchedules/*/transportCalls/*/transportCallReference")
          .filter(tcrNode -> !tcrNode.getValue().isMissingNode() && !tcrNode.getValue().isNull())
          .forEach(
              transportCallReferenceNode -> {
                String transportCallReference = transportCallReferenceNode.getValue().asText();
                if (!transportCallReferences.add(transportCallReference)) {
                  errors.add(
                      ("Duplicate transportCallReference %s " + "found at %s")
                          .formatted(transportCallReference, transportCallReferenceNode.getKey()));
                }
              });
    }
    return errors;
  }

  public Stream<Map.Entry<String, JsonNode>> findMatchingNodes(JsonNode node, String jsonPath) {
    return findMatchingNodes(node, jsonPath, "");
  }

  private Stream<Map.Entry<String, JsonNode>> findMatchingNodes(
      JsonNode node, String jsonPath, String currentPath) {
    if (jsonPath.isEmpty() || jsonPath.equals("/")) {
      return Stream.of(Map.entry(currentPath, node));
    }

    String[] pathSegments = jsonPath.split("/", 2);
    String segment = pathSegments[0];
    String remainingPath = pathSegments.length > 1 ? pathSegments[1] : "";

    if (segment.equals("*")) {
      if (node.isArray()) {
        List<Map.Entry<String, JsonNode>> results = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
          JsonNode childNode = node.get(i);
          String newPath = currentPath.isEmpty() ? String.valueOf(i) : currentPath + "/" + i;
          results.addAll(findMatchingNodes(childNode, remainingPath, newPath).toList());
        }
        return results.stream();
      } else if (node.isObject()) {
        return findMatchingNodes(node, remainingPath, currentPath);
      } else {
        return Stream.of();
      }
    } else {
      JsonNode childNode = node.path(segment);
      if (!childNode.isMissingNode()) {
        String newPath = currentPath.isEmpty() ? segment : currentPath + "/" + segment;
        return findMatchingNodes(childNode, remainingPath, newPath);
      } else {
        return Stream.of();
      }
    }
  }

  public Set<String> checkServiceSchedulesExist(JsonNode body) {

    if (body == null || body.isMissingNode() || body.isNull()) {
      return Set.of("Response body is missing or null.");
    } else {
      boolean hasVesselSchedules =
          findMatchingNodes(body, "*/vesselSchedules")
              .anyMatch(
                  node ->
                      !node.getValue().isMissingNode()
                          && node.getValue().isArray()
                          && !node.getValue().isEmpty());
      if (!hasVesselSchedules) {
        return Set.of("Response doesn't have schedules.");
      }
    }
    return Set.of();
  }
}
