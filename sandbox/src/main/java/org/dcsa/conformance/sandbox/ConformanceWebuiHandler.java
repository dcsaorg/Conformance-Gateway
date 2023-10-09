package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;

@Slf4j
public class ConformanceWebuiHandler {
  public static JsonNode handleRequest(
      String environmentBaseUrl,
      String userEnvironmentId,
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      JsonNode requestNode) {
    log.info("ConformanceWebuiHandler.handleRequest(%s)".formatted(requestNode.toPrettyString()));
    String operation = requestNode.get("operation").asText();
    return switch (operation) {
      case "createSandbox" -> _createSandbox(
          environmentBaseUrl, userEnvironmentId, persistenceProvider, asyncWebClient, requestNode);
      case "getSandboxConfig" -> _getSandboxConfig(persistenceProvider, requestNode);
      case "updateSandboxConfig" -> _updateSandboxConfig(persistenceProvider, requestNode);
      case "getAvailableStandards" -> _getAvailableStandards();
      case "getAllSandboxes" -> _getAllSandboxes(persistenceProvider, userEnvironmentId);
      case "getSandbox" -> _getSandbox(persistenceProvider, userEnvironmentId, requestNode);
      case "getScenarioDigests" -> _getScenarioDigests(persistenceProvider, requestNode);
      case "getScenario" -> _getScenario(persistenceProvider, requestNode);
      case "getScenarioStatus" -> _getScenarioStatus(persistenceProvider, requestNode);
      case "handleActionInput" -> _handleActionInput(
          persistenceProvider, asyncWebClient, requestNode);
      case "startOrStopScenario" -> _startOrStopScenario(
          persistenceProvider, asyncWebClient, requestNode);
      default -> throw new UnsupportedOperationException(operation);
    };
  }

  private static JsonNode _createSandbox(
      String environmentBaseUrl,
      String userEnvironmentId,
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      JsonNode requestNode) {
    String sandboxId = UUID.randomUUID().toString();
    String sandboxName = requestNode.get("sandboxName").asText();

    if (StreamSupport.stream(_getAllSandboxes(persistenceProvider, userEnvironmentId).spliterator(), false)
        .anyMatch(existingSandbox -> sandboxName.equals(existingSandbox.get("name").asText())))
      throw new IllegalArgumentException(
          "A sandbox named '%s' already exists".formatted(sandboxName));

    String standardName = requestNode.get("standardName").asText();
    if (!"eBL Surrender".equals(standardName))
      throw new IllegalArgumentException("Unsupported standard: " + standardName);

    String versionNumber = requestNode.get("versionNumber").asText();
    if (!Set.of("2.0-beta1", "3.0-beta1").contains(versionNumber))
      throw new IllegalArgumentException("Unsupported version: " + versionNumber);

    String testedPartyRole = requestNode.get("testedPartyRole").asText();
    if (!Set.of("Carrier", "Platform").contains(testedPartyRole))
      throw new IllegalArgumentException("Unsupported role: " + testedPartyRole);

    boolean isDefaultType = requestNode.get("isDefaultType").asBoolean();

    String baseConfigFileName =
        "manual-%s-%s"
            .formatted(
                testedPartyRole.toLowerCase(),
                isDefaultType ? "testing-counterparts" : "tested-party");

    SandboxConfiguration sandboxConfiguration =
        SandboxConfiguration.fromJsonNode(
            JsonToolkit.inputStreamToJsonNode(
                ConformanceWebuiHandler.class.getResourceAsStream(
                    "/standards/eblsurrender/v10/%s.json".formatted(baseConfigFileName))));

    sandboxConfiguration.setId(sandboxId);
    sandboxConfiguration.setName(sandboxName);
    sandboxConfiguration.setAuthHeaderName("dcsa-conformance-api-key");
    sandboxConfiguration.setAuthHeaderValue(UUID.randomUUID().toString());

    PartyConfiguration sandboxPartyConfig =
        Arrays.stream(sandboxConfiguration.getParties())
            .filter(party -> isDefaultType != party.getRole().equals(testedPartyRole))
            .findFirst()
            .orElseThrow();

    sandboxPartyConfig.setOrchestratorUrl(
        isDefaultType ? "%s/conformance/sandbox/%s".formatted(environmentBaseUrl, sandboxId) : "");

    CounterpartConfiguration sandboxPartyCounterpartConfig =
        Arrays.stream(sandboxConfiguration.getCounterparts())
            .filter(
                counterpart ->
                    Arrays.stream(sandboxConfiguration.getParties())
                        .anyMatch(party -> party.getName().equals(counterpart.getName())))
            .findFirst()
            .orElseThrow();

    sandboxPartyCounterpartConfig.setUrl(
        "%s/conformance/sandbox/%s/party/%s/api"
            .formatted(environmentBaseUrl, sandboxId, sandboxPartyCounterpartConfig.getName()));
    sandboxPartyCounterpartConfig.setAuthHeaderName(sandboxConfiguration.getAuthHeaderName());
    sandboxPartyCounterpartConfig.setAuthHeaderValue(sandboxConfiguration.getAuthHeaderValue());

    CounterpartConfiguration externalPartyCounterpartConfig =
        Arrays.stream(sandboxConfiguration.getCounterparts())
            .filter(
                counterpart ->
                    Arrays.stream(sandboxConfiguration.getParties())
                        .noneMatch(party -> party.getName().equals(counterpart.getName())))
            .findFirst()
            .orElseThrow();

    externalPartyCounterpartConfig.setUrl("");

    ConformanceSandbox.create(
        persistenceProvider,
        asyncWebClient,
        userEnvironmentId,
        sandboxId,
        sandboxName,
        sandboxConfiguration);

    log.info("Created sandbox: " + sandboxConfiguration.toJsonNode().toPrettyString());

    return new ObjectMapper().createObjectNode().put("sandboxId", sandboxId);
  }

  private static JsonNode _getSandboxConfig(
      ConformancePersistenceProvider persistenceProvider, JsonNode requestNode) {
    SandboxConfiguration sandboxConfiguration =
        ConformanceSandbox.loadSandboxConfiguration(
            persistenceProvider, requestNode.get("sandboxId").asText());

    CounterpartConfiguration sandboxPartyCounterpartConfig =
        Arrays.stream(sandboxConfiguration.getCounterparts())
            .filter(
                counterpart ->
                    Arrays.stream(sandboxConfiguration.getParties())
                        .anyMatch(party -> party.getName().equals(counterpart.getName())))
            .findFirst()
            .orElseThrow();

    CounterpartConfiguration externalPartyCounterpartConfig =
        Arrays.stream(sandboxConfiguration.getCounterparts())
            .filter(
                counterpart ->
                    Arrays.stream(sandboxConfiguration.getParties())
                        .noneMatch(party -> party.getName().equals(counterpart.getName())))
            .findFirst()
            .orElseThrow();

    return new ObjectMapper()
        .createObjectNode()
        .put("sandboxId", sandboxConfiguration.getId())
        .put("sandboxName", sandboxConfiguration.getName())
        .put("sandboxUrl", sandboxPartyCounterpartConfig.getUrl())
        .put("sandboxAuthHeaderName", sandboxConfiguration.getAuthHeaderName())
        .put("sandboxAuthHeaderValue", sandboxConfiguration.getAuthHeaderValue())
        .put("externalPartyUrl", externalPartyCounterpartConfig.getUrl())
        .put("externalPartyAuthHeaderName", externalPartyCounterpartConfig.getAuthHeaderName())
        .put("externalPartyAuthHeaderValue", externalPartyCounterpartConfig.getAuthHeaderValue());
  }

  private static JsonNode _updateSandboxConfig(
      ConformancePersistenceProvider persistenceProvider, JsonNode requestNode) {
    SandboxConfiguration sandboxConfiguration =
        ConformanceSandbox.loadSandboxConfiguration(
            persistenceProvider, requestNode.get("sandboxId").asText());

    log.info("Updating sandbox: " + sandboxConfiguration.toJsonNode().toPrettyString());

    CounterpartConfiguration externalPartyCounterpartConfig =
        Arrays.stream(sandboxConfiguration.getCounterparts())
            .filter(
                counterpart ->
                    Arrays.stream(sandboxConfiguration.getParties())
                        .noneMatch(party -> party.getName().equals(counterpart.getName())))
            .findFirst()
            .orElseThrow();

    sandboxConfiguration.setName(requestNode.get("sandboxName").asText());
    externalPartyCounterpartConfig.setUrl(requestNode.get("externalPartyUrl").asText());
    externalPartyCounterpartConfig.setAuthHeaderName(
        requestNode.get("externalPartyAuthHeaderName").asText());
    externalPartyCounterpartConfig.setAuthHeaderValue(
        requestNode.get("externalPartyAuthHeaderValue").asText());

    if (!sandboxConfiguration.getOrchestrator().isActive()) {
      Arrays.stream(sandboxConfiguration.getParties())
          .findFirst()
          .orElseThrow()
          .setOrchestratorUrl(
              externalPartyCounterpartConfig
                  .getUrl()
                  .substring(
                      0,
                      externalPartyCounterpartConfig.getUrl().length()
                          - "/party/%s/api"
                              .formatted(externalPartyCounterpartConfig.getName())
                              .length()));
    }

    ConformanceSandbox.saveSandboxConfiguration(persistenceProvider, sandboxConfiguration);

    log.info("Updated sandbox: " + sandboxConfiguration.toJsonNode().toPrettyString());

    return new ObjectMapper().createObjectNode();
  }

  private static JsonNode _getAvailableStandards() {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode standardsNode = objectMapper.createArrayNode();
    for (String standardName : new String[] {"eBL Issuance", "eBL Surrender"}) {
      ObjectNode eblSurrenderNode = objectMapper.createObjectNode().put("name", standardName);
      standardsNode.add(eblSurrenderNode);
      {
        ArrayNode eblSurrenderVersionsNode = objectMapper.createArrayNode();
        eblSurrenderNode.set("versions", eblSurrenderVersionsNode);
        for (String versionNumber : new String[] {"2.0-beta1", "3.0-beta1"}) {
          ObjectNode versionNode = objectMapper.createObjectNode().put("number", versionNumber);
          eblSurrenderVersionsNode.add(versionNode);
          {
            ArrayNode rolesNode = objectMapper.createArrayNode();
            versionNode.set("roles", rolesNode);
            rolesNode.add("Carrier");
            rolesNode.add("Platform");
          }
        }
      }
    }
    return standardsNode;
  }

  private static JsonNode _getAllSandboxes(
      ConformancePersistenceProvider persistenceProvider, String environmentId) {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode sandboxesNode = objectMapper.createArrayNode();
    persistenceProvider
        .getNonLockingMap()
        .getPartitionValues("environment#" + environmentId)
        .forEach(sandboxesNode::add);
    return sandboxesNode;
  }

  private static JsonNode _getSandbox(
      ConformancePersistenceProvider persistenceProvider,
      String environmentId,
      JsonNode requestNode) {
    return persistenceProvider
        .getNonLockingMap()
        .getItemValue(
            "environment#" + environmentId, "sandbox#" + requestNode.get("sandboxId").asText());
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
