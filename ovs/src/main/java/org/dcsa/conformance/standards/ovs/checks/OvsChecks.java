package org.dcsa.conformance.standards.ovs.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Supplier;
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
