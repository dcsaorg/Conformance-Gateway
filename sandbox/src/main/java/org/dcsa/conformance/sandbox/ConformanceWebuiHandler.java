package org.dcsa.conformance.sandbox;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.HttpHeaderConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;

@Slf4j
public class ConformanceWebuiHandler {
  private final ConformanceAccessChecker accessChecker;
  private final String environmentBaseUrl;
  private final ConformancePersistenceProvider persistenceProvider;
  private final Consumer<JsonNode> deferredSandboxTaskConsumer;

  private final SortedMap<String, ? extends AbstractStandard> standardsByName =
      new TreeMap<>(
          Arrays.stream(ConformanceSandbox.SUPPORTED_STANDARDS)
              .collect(Collectors.toMap(AbstractStandard::getName, Function.identity())));

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
    try {
      return _doHandleRequest(userId, requestNode);
    } catch (Exception e) {
      if (e instanceof UserFacingException userFacingException) {
        return OBJECT_MAPPER.createObjectNode().put("error", userFacingException.getMessage());
      } else {
        return OBJECT_MAPPER.createObjectNode().put("error", "Internal Server Error");
      }
    }
  }

  public JsonNode _doHandleRequest(String userId, JsonNode requestNode) {
    log.info("ConformanceWebuiHandler.handleRequest(%s)".formatted(requestNode.toPrettyString()));
    String operation = requestNode.get("operation").asText();
    JsonNode resultNode = switch (operation) {
      case "createSandbox" -> _createSandbox(userId, requestNode);
      case "getSandboxConfig" -> _getSandboxConfig(userId, requestNode);
      case "getSandboxStatus" -> _getSandboxStatus(userId, requestNode);
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
      case "completeCurrentAction" -> _completeCurrentAction(userId, requestNode);
      default -> throw new UnsupportedOperationException(operation);
    };
    log.debug("ConformanceWebuiHandler.handleRequest() returning: %s".formatted(resultNode.toPrettyString()));
    return resultNode;
  }

  private JsonNode _createSandbox(String userId, JsonNode requestNode) {
    String sandboxId = UUID.randomUUID().toString();
    String sandboxName = requestNode.get("sandboxName").asText();

    if (StreamSupport.stream(_getAllSandboxes(userId).spliterator(), false)
        .anyMatch(existingSandbox -> sandboxName.equals(existingSandbox.get("name").asText())))
      throw new IllegalArgumentException(
          "A sandbox named '%s' already exists".formatted(sandboxName));

    String standardName = requestNode.get("standardName").asText();
    AbstractStandard standard = standardsByName.get(standardName);
    if (standard == null)
      throw new IllegalArgumentException("Unsupported standard '%s'".formatted(standardName));

    String versionNumber = requestNode.get("versionNumber").asText();
    SortedSet<String> availableScenarioSuites = standard.getScenarioSuitesByStandardVersion().get(versionNumber);
    if (availableScenarioSuites == null)
      throw new IllegalArgumentException("Unsupported version '%s'".formatted(versionNumber));

    String scenarioSuite = requestNode.get("scenarioSuite").asText();
    if (!availableScenarioSuites.contains(scenarioSuite))
      throw new IllegalArgumentException("Unsupported scenario suite '%s'".formatted(scenarioSuite));

    AbstractComponentFactory componentFactory = standard.createComponentFactory(versionNumber, scenarioSuite);
    String testedPartyRole = requestNode.get("testedPartyRole").asText();
    if (!componentFactory
        .getRoleNames()
        .contains(testedPartyRole))
      throw new IllegalArgumentException("Unsupported role: " + testedPartyRole);

    boolean isDefaultType = requestNode.get("isDefaultType").asBoolean();

    SandboxConfiguration sandboxConfiguration =
        SandboxConfiguration.fromJsonNode(
            componentFactory.getJsonSandboxConfigurationTemplate(
                testedPartyRole, true, isDefaultType));

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

    ObjectNode jsonSandboxConfig = OBJECT_MAPPER
      .createObjectNode()
      .put("sandboxId", sandboxConfiguration.getId())
      .put("sandboxName", sandboxConfiguration.getName())
      .put("sandboxUrl", sandboxPartyCounterpartConfig.getUrl())
      .put("sandboxAuthHeaderName", sandboxConfiguration.getAuthHeaderName())
      .put("sandboxAuthHeaderValue", sandboxConfiguration.getAuthHeaderValue())
      .put("externalPartyUrl", externalPartyCounterpartConfig.getUrl())
      .put("externalPartyAuthHeaderName", externalPartyCounterpartConfig.getAuthHeaderName())
      .put("externalPartyAuthHeaderValue", externalPartyCounterpartConfig.getAuthHeaderValue());

    ArrayNode jsonAdditionalHeaders = OBJECT_MAPPER.createArrayNode();
    HttpHeaderConfiguration[] additionalHeaders =
        externalPartyCounterpartConfig.getExternalPartyAdditionalHeaders();
    if (additionalHeaders != null) {
      Arrays.stream(additionalHeaders)
          .forEach(
              headerNameAndValue -> jsonAdditionalHeaders.add(headerNameAndValue.toJsonNode()));
    }
    jsonSandboxConfig.set("externalPartyAdditionalHeaders", jsonAdditionalHeaders);

    return jsonSandboxConfig;
  }

  private JsonNode _getSandboxStatus(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    return ConformanceSandbox.getSandboxStatus(persistenceProvider, sandboxId);
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

    String externalPartyUrl = requestNode.get("externalPartyUrl").asText();
    String sandboxPartyBaseUrl =
        Stream.of(sandboxConfiguration.getCounterparts())
            .filter(
                counterpart ->
                    counterpart.getName().equals(sandboxConfiguration.getParties()[0].getName()))
            .findFirst()
            .orElseThrow()
            .getUrl()
            .split("/party/")[0] + "/";
    CounterpartConfiguration.validateUrl(
        externalPartyUrl, sandboxPartyBaseUrl.startsWith("http://localhost"));
    if (externalPartyUrl.startsWith(sandboxPartyBaseUrl))
      throw new UserFacingException("The sandbox URL cannot be used as external party URL");

    sandboxConfiguration.setName(requestNode.get("sandboxName").asText());
    externalPartyCounterpartConfig.setUrl(externalPartyUrl);
    externalPartyCounterpartConfig.setAuthHeaderName(
        requestNode.get("externalPartyAuthHeaderName").asText());
    externalPartyCounterpartConfig.setAuthHeaderValue(
        requestNode.get("externalPartyAuthHeaderValue").asText());
    JsonNode jsonHeaders = requestNode.get("externalPartyAdditionalHeaders");
    if (jsonHeaders.isArray()) {
      externalPartyCounterpartConfig.setExternalPartyAdditionalHeaders(
          StreamSupport.stream(jsonHeaders.spliterator(), false)
              .map(HttpHeaderConfiguration::fromJsonNode)
              .toArray(HttpHeaderConfiguration[]::new));
    }

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
    /*
    [
      {
        "name": "Booking",
        "versions": [
            {
              "number": "2.0.0",
              "suites": [
                "Conformance",
                "Reference Implementation"
              ],
              "roles": [
                "Carrier",
                "Shipper"
              ]
            }
        ]
      },
     */
    ArrayNode standardsNode = OBJECT_MAPPER.createArrayNode();
    TreeSet<String> sortedStandardNames = new TreeSet<>(String::compareToIgnoreCase);
    sortedStandardNames.addAll(standardsByName.keySet());
    sortedStandardNames.forEach(
        standardName -> {
          ObjectNode standardNode = OBJECT_MAPPER.createObjectNode().put("name", standardName);
          standardsNode.add(standardNode);
          {
            ArrayNode versionsNode = OBJECT_MAPPER.createArrayNode();
            standardNode.set("versions", versionsNode);
            AbstractStandard standard = standardsByName.get(standardName);
            SortedMap<String, SortedSet<String>> scenarioSuitesByStandardVersion =
                standard.getScenarioSuitesByStandardVersion();
            scenarioSuitesByStandardVersion
                .keySet()
                .forEach(
                    standardVersion -> {
                      ObjectNode versionNode =
                          OBJECT_MAPPER.createObjectNode().put("number", standardVersion);
                      versionsNode.add(versionNode);

                      SortedSet<String> scenarioSuites =
                          scenarioSuitesByStandardVersion.get(standardVersion);
                      { // scoped
                        ArrayNode suitesNode = OBJECT_MAPPER.createArrayNode();
                        versionNode.set("suites", suitesNode);
                        scenarioSuites.forEach(suitesNode::add);
                      }
                      { // scoped
                        ArrayNode rolesNode = OBJECT_MAPPER.createArrayNode();
                        versionNode.set("roles", rolesNode);
                        standard
                            .createComponentFactory(standardVersion, scenarioSuites.first())
                            .getRoleNames()
                            .forEach(rolesNode::add);
                      }
                    });
          }
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
    ConformanceSandbox.startOrStopScenario(
        persistenceProvider,
        deferredSandboxTaskConsumer,
        sandboxId,
        requestNode.get("scenarioId").asText());
    ConformanceSandbox.resetParty(persistenceProvider, deferredSandboxTaskConsumer, sandboxId);
    return OBJECT_MAPPER.createObjectNode();
  }

  private JsonNode _completeCurrentAction(String userId, JsonNode requestNode) {
    String sandboxId = requestNode.get("sandboxId").asText();
    accessChecker.checkUserSandboxAccess(userId, sandboxId);
    return ConformanceSandbox.completeCurrentAction(
        persistenceProvider, deferredSandboxTaskConsumer, sandboxId);
  }
}
