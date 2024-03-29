package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;
import org.dcsa.conformance.standards.booking.BookingComponentFactory;
import org.dcsa.conformance.standards.ebl.EblComponentFactory;
import org.dcsa.conformance.standards.eblinterop.PintComponentFactory;
import org.dcsa.conformance.standards.eblissuance.EblIssuanceComponentFactory;
import org.dcsa.conformance.standards.eblsurrender.EblSurrenderComponentFactory;
import org.dcsa.conformance.standards.jit.JitComponentFactory;
import org.dcsa.conformance.standards.ovs.OvsComponentFactory;
import org.dcsa.conformance.standards.tnt.TntComponentFactory;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Slf4j
public class ConformanceWebuiHandler {
  private final ConformanceAccessChecker accessChecker;
  private final String environmentBaseUrl;
  private final ConformancePersistenceProvider persistenceProvider;
  private final Consumer<JsonNode> deferredSandboxTaskConsumer;

  private final SortedMap<String, SortedMap<String, AbstractComponentFactory>> componentFactories =
      new TreeMap<>(
          Map.ofEntries(
              Map.entry(
                  BookingComponentFactory.STANDARD_NAME,
                  new TreeMap<>(
                      BookingComponentFactory.STANDARD_VERSIONS.stream()
                          .collect(
                              Collectors.toMap(
                                  Function.identity(), BookingComponentFactory::new)))),
              Map.entry(
                  EblComponentFactory.STANDARD_NAME,
                  new TreeMap<>(
                      EblComponentFactory.STANDARD_VERSIONS.stream()
                          .collect(
                              Collectors.toMap(Function.identity(), EblComponentFactory::new)))),
              Map.entry(
                  EblIssuanceComponentFactory.STANDARD_NAME,
                  new TreeMap<>(
                      EblIssuanceComponentFactory.STANDARD_VERSIONS.stream()
                          .collect(
                              Collectors.toMap(
                                  Function.identity(), EblIssuanceComponentFactory::new)))),
              Map.entry(
                  EblSurrenderComponentFactory.STANDARD_NAME,
                  new TreeMap<>(
                      EblSurrenderComponentFactory.STANDARD_VERSIONS.stream()
                          .collect(
                              Collectors.toMap(
                                  Function.identity(), EblSurrenderComponentFactory::new)))),
              Map.entry(
                  JitComponentFactory.STANDARD_NAME,
                  new TreeMap<>(
                      JitComponentFactory.STANDARD_VERSIONS.stream()
                          .collect(
                              Collectors.toMap(Function.identity(), JitComponentFactory::new)))),
              Map.entry(
                  OvsComponentFactory.STANDARD_NAME,
                  new TreeMap<>(
                      OvsComponentFactory.STANDARD_VERSIONS.stream()
                          .collect(
                              Collectors.toMap(Function.identity(), OvsComponentFactory::new)))),
              Map.entry(
                  PintComponentFactory.STANDARD_NAME,
                  new TreeMap<>(
                    PintComponentFactory.STANDARD_VERSIONS.stream()
                          .collect(
                              Collectors.toMap(Function.identity(), PintComponentFactory::new)))),
              Map.entry(
                  TntComponentFactory.STANDARD_NAME,
                  new TreeMap<>(
                      TntComponentFactory.STANDARD_VERSIONS.stream()
                          .collect(
                              Collectors.toMap(Function.identity(), TntComponentFactory::new))))));

  public ConformanceWebuiHandler(
      ConformanceAccessChecker accessChecker,
      String environmentBaseUrl,
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer) {
    this.accessChecker = accessChecker;
    this.environmentBaseUrl = environmentBaseUrl;
    this.persistenceProvider = persistenceProvider;
    this.deferredSandboxTaskConsumer = deferredSandboxTaskConsumer;
  }

  public JsonNode handleRequest(String userId, JsonNode requestNode) {
    log.info("ConformanceWebuiHandler.handleRequest(%s)".formatted(requestNode.toPrettyString()));
    String operation = requestNode.get("operation").asText();
    return switch (operation) {
      case "createSandbox" -> _createSandbox(userId, requestNode);
      case "getSandboxConfig" -> _getSandboxConfig(userId, requestNode);
      case "updateSandboxConfig" -> _updateSandboxConfig(userId, requestNode);
      case "getAvailableStandards" -> _getAvailableStandards(userId);
      case "getAllSandboxes" -> _getAllSandboxes(userId);
      case "getSandbox" -> _getSandbox(userId, requestNode);
      case "notifyParty" -> _notifyParty(userId, requestNode);
      case "resetParty" -> _resetParty(userId, requestNode);
      case "getScenarioDigests" -> _getScenarioDigests(userId, requestNode);
      case "getScenario" -> _getScenario(userId, requestNode);
      case "getScenarioStatus" -> _getScenarioStatus(userId, requestNode);
      case "handleActionInput" -> _handleActionInput(userId, requestNode);
      case "startOrStopScenario" -> _startOrStopScenario(userId, requestNode);
      default -> throw new UnsupportedOperationException(operation);
    };
  }

  private JsonNode _createSandbox(String userId, JsonNode requestNode) {
    String sandboxId = UUID.randomUUID().toString();
    String sandboxName = requestNode.get("sandboxName").asText();

    if (StreamSupport.stream(_getAllSandboxes(userId).spliterator(), false)
        .anyMatch(existingSandbox -> sandboxName.equals(existingSandbox.get("name").asText())))
      throw new IllegalArgumentException(
          "A sandbox named '%s' already exists".formatted(sandboxName));

    String standardName = requestNode.get("standardName").asText();
    if (!componentFactories.containsKey(standardName))
      throw new IllegalArgumentException("Unsupported standard: " + standardName);

    String versionNumber = requestNode.get("versionNumber").asText();
    if (!componentFactories.get(standardName).containsKey(versionNumber))
      throw new IllegalArgumentException("Unsupported version: " + versionNumber);

    String testedPartyRole = requestNode.get("testedPartyRole").asText();
    if (!componentFactories
        .get(standardName)
        .get(versionNumber)
        .getRoleNames()
        .contains(testedPartyRole))
      throw new IllegalArgumentException("Unsupported role: " + testedPartyRole);

    boolean isDefaultType = requestNode.get("isDefaultType").asBoolean();

    SandboxConfiguration sandboxConfiguration =
        SandboxConfiguration.fromJsonNode(
            componentFactories
                .get(standardName)
                .get(versionNumber)
                .getJsonSandboxConfigurationTemplate(testedPartyRole, true, isDefaultType));

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
        deferredSandboxTaskConsumer,
        accessChecker.getUserEnvironmentId(userId),
        sandboxConfiguration);

    log.info("Created sandbox: " + sandboxConfiguration.toJsonNode().toPrettyString());

    return OBJECT_MAPPER.createObjectNode().put("sandboxId", sandboxId);
  }

  private JsonNode _getSandboxConfig(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    SandboxConfiguration sandboxConfiguration =
        ConformanceSandbox.loadSandboxConfiguration(persistenceProvider, sandboxId);

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

    return OBJECT_MAPPER
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

  private JsonNode _updateSandboxConfig(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    SandboxConfiguration sandboxConfiguration =
        ConformanceSandbox.loadSandboxConfiguration(persistenceProvider, sandboxId);

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

    ConformanceSandbox.saveSandboxConfiguration(persistenceProvider, userId, sandboxConfiguration);

    log.info("Updated sandbox: " + sandboxConfiguration.toJsonNode().toPrettyString());

    return OBJECT_MAPPER.createObjectNode();
  }

  private JsonNode _getAvailableStandards(String ignoredUserId) {
    ArrayNode standardsNode = OBJECT_MAPPER.createArrayNode();
    TreeSet<String> sortedStandardNames = new TreeSet<>(String::compareToIgnoreCase);
    sortedStandardNames.addAll(componentFactories.keySet());
    sortedStandardNames.forEach(
        standardName -> {
          ObjectNode standardNode = OBJECT_MAPPER.createObjectNode().put("name", standardName);
          {
            ArrayNode versionsNode = OBJECT_MAPPER.createArrayNode();
            standardNode.set("versions", versionsNode);
            componentFactories
                .get(standardName)
                .keySet()
                .forEach(
                    standardVersion -> {
                      ObjectNode versionNode =
                        OBJECT_MAPPER.createObjectNode().put("number", standardVersion);
                      {
                        ArrayNode rolesNode = OBJECT_MAPPER.createArrayNode();
                        componentFactories
                            .get(standardName)
                            .get(standardVersion)
                            .getRoleNames()
                            .forEach(rolesNode::add);
                        versionNode.set("roles", rolesNode);
                      }
                      versionsNode.add(versionNode);
                    });
          }
          standardsNode.add(standardNode);
        });
    return standardsNode;
  }

  private JsonNode _getAllSandboxes(String userId) {
    TreeMap<String, JsonNode> sortedSandboxesByLowercaseName = new TreeMap<>();
    persistenceProvider
        .getNonLockingMap()
        .getPartitionValues("environment#" + accessChecker.getUserEnvironmentId(userId))
        .forEach(
            sandboxNode ->
                sortedSandboxesByLowercaseName.put(
                    sandboxNode.get("name").asText().toLowerCase(), sandboxNode));
    ArrayNode sandboxesNode = OBJECT_MAPPER.createArrayNode();
    sortedSandboxesByLowercaseName.values().forEach(sandboxesNode::add);
    return sandboxesNode;
  }

  private JsonNode _getSandbox(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    ObjectNode sandboxNode =
        (ObjectNode)
            persistenceProvider
                .getNonLockingMap()
                .getItemValue(
                    "environment#" + accessChecker.getUserEnvironmentId(userId),
                    "sandbox#" + sandboxId);

    boolean includeOperatorLog = requestNode.get("includeOperatorLog").asBoolean();
    if (includeOperatorLog) {
      ArrayNode operatorLog =
          ConformanceSandbox.getOperatorLog(
              persistenceProvider, deferredSandboxTaskConsumer, sandboxId);
      sandboxNode.set("operatorLog", operatorLog);
      sandboxNode.put("canNotifyParty", operatorLog != null);
    }
    return sandboxNode;
  }

  private JsonNode _notifyParty(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    ConformanceSandbox.notifyParty(persistenceProvider, deferredSandboxTaskConsumer, sandboxId);
    return OBJECT_MAPPER.createObjectNode();
  }

  private JsonNode _resetParty(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    ConformanceSandbox.resetParty(persistenceProvider, deferredSandboxTaskConsumer, sandboxId);
    return OBJECT_MAPPER.createObjectNode();
  }

  private JsonNode _getScenarioDigests(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    return ConformanceSandbox.getScenarioDigests(persistenceProvider, sandboxId);
  }

  private JsonNode _getScenario(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    return ConformanceSandbox.getScenarioDigest(
        persistenceProvider, sandboxId, requestNode.get("scenarioId").asText());
  }

  private JsonNode _getScenarioStatus(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    return ConformanceSandbox.getScenarioStatus(
        persistenceProvider, sandboxId, requestNode.get("scenarioId").asText());
  }

  private JsonNode _handleActionInput(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    JsonNode actionInputNode = requestNode.get("actionInput");
    return ConformanceSandbox.handleActionInput(
        persistenceProvider,
        deferredSandboxTaskConsumer,
        sandboxId,
        requestNode.get("actionId").asText(),
        actionInputNode);
  }

  private JsonNode _startOrStopScenario(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    return ConformanceSandbox.startOrStopScenario(
        persistenceProvider,
        deferredSandboxTaskConsumer,
        sandboxId,
        requestNode.get("scenarioId").asText());
  }
}
