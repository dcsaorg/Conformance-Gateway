package org.dcsa.conformance.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.sandbox.ConformanceSandbox;
import org.dcsa.conformance.sandbox.ConformanceWebRequest;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;

import java.util.function.Consumer;

@Slf4j
public class ConformanceWebuiHandler {
  public static JsonNode handleRequest(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      JsonNode requestNode) {
    log.info("ConformanceWebuiHandler.handleRequest(%s)".formatted(requestNode.toPrettyString()));
    String operation = requestNode.get("operation").asText();
    return switch (operation) {
      case "getAllSandboxes" -> _getAllSandboxes(persistenceProvider);
      case "getSandbox" -> _getSandbox(persistenceProvider, requestNode);
      case "getScenarioDigests" -> _getScenarioDigests(persistenceProvider, requestNode);
      case "getScenario" -> _getScenario(persistenceProvider, requestNode);
      case "getScenarioStatus" -> _getScenarioStatus(persistenceProvider, requestNode);
      case "handleActionInput" -> _handleActionInput(persistenceProvider, asyncWebClient, requestNode);
      case "startOrStopScenario" -> _startOrStopScenario(persistenceProvider, asyncWebClient, requestNode);
      default -> throw new UnsupportedOperationException(operation);
    };
  }

  private static JsonNode _getAllSandboxes(ConformancePersistenceProvider persistenceProvider) {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode sandboxesNode = objectMapper.createArrayNode();
    persistenceProvider
        .getNonLockingMap()
        .getPartitionValues("environment#spring-boot-env")
        .forEach(sandboxesNode::add);
    return sandboxesNode;
  }

  private static JsonNode _getSandbox(
      ConformancePersistenceProvider persistenceProvider, JsonNode requestNode) {
    return persistenceProvider
        .getNonLockingMap()
        .getItemValue(
            "environment#spring-boot-env", "sandbox#" + requestNode.get("sandboxId").asText());
  }

  private static JsonNode _getScenarioDigests(
      ConformancePersistenceProvider persistenceProvider, JsonNode requestNode) {
    return ConformanceSandbox.getScenarioDigests(
        persistenceProvider, requestNode.get("sandboxId").asText());
  }

  private static JsonNode _getScenario(
      ConformancePersistenceProvider persistenceProvider, JsonNode requestNode) {
    return ConformanceSandbox.getScenarioDigest(
        persistenceProvider,
        requestNode.get("sandboxId").asText(),
        requestNode.get("scenarioId").asText());
  }

  private static JsonNode _getScenarioStatus(
      ConformancePersistenceProvider persistenceProvider, JsonNode requestNode) {
    return ConformanceSandbox.getScenarioStatus(
        persistenceProvider,
        requestNode.get("sandboxId").asText(),
        requestNode.get("scenarioId").asText());
  }

  private static JsonNode _handleActionInput(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      JsonNode requestNode) {
    JsonNode actionInputNode = requestNode.get("actionInput");
    if (actionInputNode != null) {
      return ConformanceSandbox.handleActionInput(
          persistenceProvider,
          asyncWebClient,
          requestNode.get("sandboxId").asText(),
          requestNode.get("actionId").asText(),
          actionInputNode.asText());
    } else {
      return new ObjectMapper().createObjectNode();
    }
  }

  private static JsonNode _startOrStopScenario(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      JsonNode requestNode) {
    return ConformanceSandbox.startOrStopScenario(
        persistenceProvider,
        asyncWebClient,
        requestNode.get("sandboxId").asText(),
        requestNode.get("scenarioId").asText());
  }
}
