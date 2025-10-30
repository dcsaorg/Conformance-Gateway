package org.dcsa.conformance.sandbox;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.dcsa.conformance.sandbox.configuration.StandardConfiguration;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;
import org.dcsa.conformance.standards.adoption.AdoptionStandard;
import org.dcsa.conformance.standards.an.AnStandard;
import org.dcsa.conformance.standards.booking.BookingStandard;
import org.dcsa.conformance.standards.bookingandebl.BookingAndEblStandard;
import org.dcsa.conformance.standards.cs.CsStandard;
import org.dcsa.conformance.standards.ebl.EblStandard;
import org.dcsa.conformance.standards.eblinterop.PintStandard;
import org.dcsa.conformance.standards.eblissuance.EblIssuanceStandard;
import org.dcsa.conformance.standards.eblsurrender.EblSurrenderStandard;
import org.dcsa.conformance.standards.portcall.JitStandard;
import org.dcsa.conformance.standards.ovs.OvsStandard;
import org.dcsa.conformance.standards.tnt.TntStandard;

@Slf4j
public class ConformanceSandbox {
  protected static final String SANDBOX = "sandbox#";
  protected static final String SESSION = "session#";
  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public static final AbstractStandard[] SUPPORTED_STANDARDS = {
    AdoptionStandard.INSTANCE,
    AnStandard.INSTANCE,
    BookingStandard.INSTANCE,
    CsStandard.INSTANCE,
    EblStandard.INSTANCE,
    EblIssuanceStandard.INSTANCE,
    EblSurrenderStandard.INSTANCE,
    BookingAndEblStandard.INSTANCE,
    JitStandard.INSTANCE,
    OvsStandard.INSTANCE,
    PintStandard.INSTANCE,
    TntStandard.INSTANCE
  };

  private record OrchestratorTask(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      String sandboxId,
      String description,
      Consumer<ConformanceOrchestrator> orchestratorConsumer)
      implements Runnable {
    @Override
    public void run() {
      String currentSessionId =
          _loadSandboxState(persistenceProvider, sandboxId).get("currentSessionId").asText();
      persistenceProvider
          .getStatefulExecutor()
          .execute(
              description,
              SESSION + currentSessionId,
              "state#orchestrator",
              originalOrchestratorState -> {
                SandboxConfiguration sandboxConfiguration =
                    loadSandboxConfiguration(persistenceProvider, sandboxId);
                AbstractComponentFactory componentFactory =
                    _createComponentFactory(
                        sandboxConfiguration.getStandard(),
                        sandboxConfiguration.getScenarioSuite());
                ConformanceOrchestrator orchestrator =
                    new ConformanceOrchestrator(
                        sandboxConfiguration,
                        componentFactory,
                        new TrafficRecorder(
                            persistenceProvider.getNonLockingMap(), SESSION + currentSessionId),
                        new JsonNodeMap(
                            persistenceProvider.getNonLockingMap(),
                            SESSION + currentSessionId,
                            "map#orchestrator#"),
                        asyncWebClient);
                if (originalOrchestratorState != null && !originalOrchestratorState.isEmpty()) {
                  orchestrator.importJsonState(originalOrchestratorState);
                }
                orchestrator.setWaitingForBiConsumer(
                    (forWhom, toDoWhat) ->
                        _setWaitingFor(persistenceProvider, sandboxId, "Orchestrator", forWhom, toDoWhat));
                orchestratorConsumer.accept(orchestrator);
                return orchestrator.exportJsonState();
              });
    }
  }

  private record PartyTask(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      String partyName,
      String description,
      Consumer<ConformanceParty> partyConsumer)
      implements Runnable {
    @Override
    public void run() {
      JsonNode sandboxState = _loadSandboxState(persistenceProvider, sandboxId);
      String currentSessionId =
          sandboxState.isEmpty() ? "" : sandboxState.get("currentSessionId").asText();
      persistenceProvider
          .getStatefulExecutor()
          .execute(
              description,
              SESSION + currentSessionId,
              "state#party#" + partyName,
              originalPartyState -> {
                SandboxConfiguration sandboxConfiguration =
                    loadSandboxConfiguration(persistenceProvider, sandboxId);
                AbstractComponentFactory componentFactory =
                    _createComponentFactory(
                        sandboxConfiguration.getStandard(),
                        sandboxConfiguration.getScenarioSuite());

                Map<String, ? extends Collection<String>> orchestratorAuthHeader;
                if (sandboxConfiguration.getOrchestrator().isActive()) {
                  orchestratorAuthHeader =
                      sandboxConfiguration.getAuthHeaderName().isBlank()
                          ? Collections.emptyMap()
                          : Map.of(
                              sandboxConfiguration.getAuthHeaderName(),
                              List.of(sandboxConfiguration.getAuthHeaderValue()));
                } else {
                  CounterpartConfiguration externalCounterpartConfiguration =
                      Arrays.stream(sandboxConfiguration.getCounterparts())
                          .filter(
                              counterpart ->
                                  Arrays.stream(sandboxConfiguration.getParties())
                                      .noneMatch(
                                          party -> counterpart.getName().equals(party.getName())))
                          .findFirst()
                          .orElseThrow();
                  orchestratorAuthHeader =
                      externalCounterpartConfiguration.getAuthHeaderName().isBlank()
                          ? Collections.emptyMap()
                          : Map.of(
                              externalCounterpartConfiguration.getAuthHeaderName(),
                              List.of(externalCounterpartConfiguration.getAuthHeaderValue()));
                }

                PartyWebClient partyWebClient =
                    new PartyWebClient() {
                      @Override
                      public void asyncRequest(ConformanceRequest conformanceRequest) {
                        _asyncHandleOutboundRequest(
                            deferredSandboxTaskConsumer, sandboxId, conformanceRequest);
                      }

                      @Override
                      public ConformanceResponse syncRequest(
                          ConformanceRequest conformanceRequest) {
                        return _syncHandleOutboundRequest(
                            persistenceProvider,
                            deferredSandboxTaskConsumer,
                            sandboxId,
                            conformanceRequest);
                      }
                    };

                ConformanceParty party =
                    componentFactory
                        .createParties(
                            sandboxConfiguration.getParties(),
                            sandboxConfiguration.getCounterparts(),
                            new JsonNodeMap(
                                persistenceProvider.getNonLockingMap(),
                                SESSION + currentSessionId,
                                "map#party#" + partyName),
                            partyWebClient,
                            orchestratorAuthHeader)
                        .stream()
                        .filter(createdParty -> partyName.equals(createdParty.getName()))
                        .findFirst()
                        .orElseThrow(
                            () -> new IllegalArgumentException("Party not found: " + partyName));
                if (originalPartyState != null && !originalPartyState.isEmpty()) {
                  party.importJsonState(originalPartyState);
                }
                party.setWaitingForBiConsumer(
                    (forWhom, toDoWhat) ->
                        _setWaitingFor(persistenceProvider, sandboxId, partyName, forWhom, toDoWhat));
                partyConsumer.accept(party);
                return party.exportJsonState();
              });
    }
  }

  private static JsonNode _loadSandboxState(
      ConformancePersistenceProvider persistenceProvider, String sandboxId) {
    AtomicReference<JsonNode> sandboxStateNodeReference = new AtomicReference<>();
    persistenceProvider
        .getStatefulExecutor()
        .execute(
            "loading state of sandbox " + sandboxId,
            SANDBOX + sandboxId,
            "state",
            originalSandboxState -> {
              sandboxStateNodeReference.set(originalSandboxState);
              return null;
            });
    return sandboxStateNodeReference.get();
  }

  public static void create(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String environmentId,
      SandboxConfiguration sandboxConfiguration) {
    saveSandboxConfiguration(persistenceProvider, environmentId, sandboxConfiguration);
    if (!sandboxConfiguration.getOrchestrator().isActive()) {
      _handleReset(persistenceProvider, deferredSandboxTaskConsumer, sandboxConfiguration.getId());
    } else {
      if (Arrays.stream(sandboxConfiguration.getCounterparts())
          .anyMatch(CounterpartConfiguration::isInManualMode)) {
        _handleReset(
            persistenceProvider, deferredSandboxTaskConsumer, sandboxConfiguration.getId());
      }
    }
  }

  public static ConformanceWebResponse handleRequest(
      ConformancePersistenceProvider persistenceProvider,
      ConformanceWebRequest webRequest,
      Consumer<JsonNode> deferredSandboxTaskConsumer) {
    log.info(
        "ConformanceSandbox.handleRequest() {}",
        OBJECT_MAPPER.valueToTree(webRequest).toPrettyString());

    String expectedPrefix = "/conformance/sandbox/";
    int expectedPrefixStart = webRequest.url().indexOf(expectedPrefix);
    if (expectedPrefixStart < 0) {
      log.info("Rejecting request with missing '{}' in: {}", expectedPrefix, webRequest.url());
      return new ConformanceWebResponse(404, JsonToolkit.JSON_UTF_8, Collections.emptyMap(), "{}");
    }
    String remainingUri = webRequest.url().substring(expectedPrefixStart + expectedPrefix.length());

    int endOfSandboxId = remainingUri.indexOf("/");
    if (endOfSandboxId < 0) {
      throw new IllegalArgumentException("Missing sandbox id: " + webRequest.url());
    }
    String sandboxId = remainingUri.substring(0, endOfSandboxId);
    remainingUri = remainingUri.substring(endOfSandboxId);

    if (remainingUri.contains("dev/null")) {
      return new ConformanceWebResponse(204, JsonToolkit.JSON_UTF_8, Collections.emptyMap(), "{}");
    }
    SandboxConfiguration sandboxConfiguration =
        loadSandboxConfiguration(persistenceProvider, sandboxId);
    if (!sandboxConfiguration.getAuthHeaderName().isBlank()) {
      Collection<String> authHeaderValues =
          webRequest.headers().get(sandboxConfiguration.getAuthHeaderName());
      if (authHeaderValues == null || authHeaderValues.isEmpty()) {
        log.info("Authorization failed: no auth header values");
        return new ConformanceWebResponse(
            403, JsonToolkit.JSON_UTF_8, Collections.emptyMap(), "{}");
      }
      if (authHeaderValues.size() > 1) {
        log.info("Authorization failed: duplicate auth header values");
        return new ConformanceWebResponse(
            403, JsonToolkit.JSON_UTF_8, Collections.emptyMap(), "{}");
      }
      String authHeaderValue = authHeaderValues.stream().findFirst().orElseThrow();
      if (!Objects.equals(authHeaderValue, sandboxConfiguration.getAuthHeaderValue())) {
        log.info("Authorization failed: incorrect auth header value");
        return new ConformanceWebResponse(
            403, JsonToolkit.JSON_UTF_8, Collections.emptyMap(), "{}");
      }
    }

    if (remainingUri.startsWith("/party/")) {
      remainingUri = remainingUri.substring("/party/".length());
      int endOfPartyName = remainingUri.indexOf("/");
      if (endOfPartyName < 0) {
        throw new IllegalArgumentException("Missing party name: " + webRequest.url());
      }
      String partyName =
          URLDecoder.decode(remainingUri.substring(0, endOfPartyName), StandardCharsets.UTF_8);
      remainingUri = remainingUri.substring(endOfPartyName);
      if (remainingUri.equals("/api/conformance/notification")) {
        return _handlePartyNotification(
            persistenceProvider, deferredSandboxTaskConsumer, sandboxId, partyName);
      } else if (remainingUri.equals("/prompt/json")) {
        return _handleGetPartyPrompt(
            persistenceProvider, deferredSandboxTaskConsumer, sandboxId, partyName);
      } else if (remainingUri.equals("/input")) {
        return _handlePostPartyInput(
            persistenceProvider,
            deferredSandboxTaskConsumer,
            sandboxId,
            partyName,
            webRequest.body());
      } else if (remainingUri.startsWith("/api")) {
        return _handlePartyInboundConformanceRequest(
            persistenceProvider, deferredSandboxTaskConsumer, sandboxId, partyName, webRequest);
      }
    } else if (remainingUri.equals("/status")) {
      return _handleGetStatus(persistenceProvider, deferredSandboxTaskConsumer, sandboxId);
    } else if (remainingUri.equals("/report")) {
      return _handleGenerateReport(
          persistenceProvider, deferredSandboxTaskConsumer, sandboxId, false);
    } else if (remainingUri.equals("/printableReport")) {
      return _handleGenerateReport(
          persistenceProvider, deferredSandboxTaskConsumer, sandboxId, true);
    } else if (remainingUri.equals("/reset")) {
      return _handleReset(persistenceProvider, deferredSandboxTaskConsumer, sandboxId);
    }
    throw new IllegalArgumentException("Unhandled URI: " + webRequest.url());
  }

  public static ArrayNode getScenarioDigests(
      ConformancePersistenceProvider persistenceProvider, String sandboxId) {
    AtomicReference<ArrayNode> arrayNodeReference = new AtomicReference<>();
    new OrchestratorTask(
            persistenceProvider,
            null,
            sandboxId,
            "getting scenario digests for sandbox " + sandboxId,
            orchestrator -> arrayNodeReference.set(orchestrator.getScenarioDigests()))
        .run();
    return arrayNodeReference.get();
  }

  public static ArrayNode getOperatorLog(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId) {
    SandboxConfiguration sandboxConfiguration =
        loadSandboxConfiguration(persistenceProvider, sandboxId);
    if (sandboxConfiguration.getOrchestrator().isActive()) return null;

    String partyName = sandboxConfiguration.getParties()[0].getName();
    ArrayNode operatorLogNode = OBJECT_MAPPER.createArrayNode();
    new PartyTask(
            persistenceProvider,
            deferredSandboxTaskConsumer,
            sandboxId,
            partyName,
            "getting operator log for party " + partyName,
            party ->
                party.getOperatorLog().forEach(entry -> operatorLogNode.add(entry.message())))
        .run();
    return operatorLogNode;
  }

  public static void notifyParty(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId) {
    SandboxConfiguration sandboxConfiguration =
        loadSandboxConfiguration(persistenceProvider, sandboxId);
    if (sandboxConfiguration.getOrchestrator().isActive()) return;

    String partyName = sandboxConfiguration.getParties()[0].getName();
    new PartyTask(
            persistenceProvider,
            deferredSandboxTaskConsumer,
            sandboxId,
            partyName,
            "handling notification for party " + partyName,
            ConformanceParty::handleNotification)
        .run();
  }

  public static void resetParty(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId) {
    SandboxConfiguration sandboxConfiguration =
        loadSandboxConfiguration(persistenceProvider, sandboxId);
    String partyName = sandboxConfiguration.getParties()[0].getName();
    new PartyTask(
            persistenceProvider,
            deferredSandboxTaskConsumer,
            sandboxId,
            partyName,
            "resetting party " + partyName,
            ConformanceParty::reset)
        .run();
  }

  public static void createReport(
      ConformancePersistenceProvider persistenceProvider,
      String environmentId,
      SandboxConfiguration sandboxConfiguration,
      String reportTitle) {
    AtomicReference<JsonNode> resultReference = new AtomicReference<>();
    String sandboxId = sandboxConfiguration.getId();
    new OrchestratorTask(
            persistenceProvider,
            null,
            sandboxId,
            "creating for sandbox %s a full report".formatted(sandboxId),
            orchestrator -> resultReference.set(orchestrator.createFullReport()))
        .run();

    // PK=environment#UUID
    // SK=report#digest#<sandboxUUID>#<reportUTC>
    // value={...title...standard...}
    Instant reportInstant = Instant.now();
    String reportIsoTimestamp = reportInstant.toString();
    String reportDateTime = reportInstant.atZone(ZoneOffset.UTC).format(DATE_TIME_FORMATTER);
    persistenceProvider
        .getNonLockingMap()
        .setItemValue(
            "environment#" + environmentId,
            "report#digest#%s#%s".formatted(sandboxId, reportIsoTimestamp),
            OBJECT_MAPPER
                .createObjectNode()
                .put("isoTimestamp", reportIsoTimestamp)
                .put("dateTime", reportDateTime)
                .put("title", reportTitle)
                .put("standardName", sandboxConfiguration.getStandard().getName())
                .put("standardVersion", sandboxConfiguration.getStandard().getVersion())
                .put("scenarioSuite", sandboxConfiguration.getScenarioSuite())
                .put(
                    "testedPartyRole",
                    sandboxConfiguration.getSandboxPartyCounterpartConfiguration().getRole()));

    // PK=environment#UUID
    // SK=report#content#<sandboxUUID>#<reportUTC>
    // value={...report content...}
    persistenceProvider
        .getNonLockingMap()
        .setItemValue(
            "environment#" + environmentId,
            "report#content#%s#%s".formatted(sandboxId, reportIsoTimestamp),
            resultReference.get());
  }

  public static JsonNode getReportContent(
      ConformancePersistenceProvider persistenceProvider,
      String environmentId,
      String sandboxId,
      String reportIsoTimestamp) {
    return persistenceProvider
        .getNonLockingMap()
        .getItemValue(
            "environment#" + environmentId,
            "report#content#%s#%s".formatted(sandboxId, reportIsoTimestamp));
  }

  public static JsonNode getReportDigests(
    ConformancePersistenceProvider persistenceProvider, String environmentId, String sandboxId) {
    // PK=environment#UUID
    // SK=report#digest#<sandboxUUID>#<reportUTC>
    // value={...title...standard...}
    return persistenceProvider
        .getNonLockingMap()
        .getPartitionValuesBySortKey(
            "environment#" + environmentId, "report#digest#%s#".formatted(sandboxId))
        .sequencedEntrySet()
        .reversed()
        .stream()
        .map(sortKeyAndValue -> (ObjectNode) sortKeyAndValue.getValue())
        .collect(OBJECT_MAPPER::createArrayNode, ArrayNode::add, ArrayNode::addAll);
  }

  public static ObjectNode getScenarioDigest(
      ConformancePersistenceProvider persistenceProvider, String sandboxId, String scenarioId) {
    AtomicReference<ObjectNode> resultReference = new AtomicReference<>();
    new OrchestratorTask(
            persistenceProvider,
            null,
            sandboxId,
            "getting from sandbox %s the digest of scenario %s".formatted(sandboxId, scenarioId),
            orchestrator -> resultReference.set(orchestrator.getScenarioDigest(scenarioId)))
        .run();
    return resultReference.get();
  }

  public static ObjectNode getSandboxStatus(
      ConformancePersistenceProvider persistenceProvider, String sandboxId) {
    ArrayNode waitingArrayNode = OBJECT_MAPPER.createArrayNode();
    _getWaitingFor(persistenceProvider, sandboxId)
        .forEach(waiting -> waitingArrayNode.add(waiting.toJson()));
    return OBJECT_MAPPER.createObjectNode().set("waiting", waitingArrayNode);
  }

  private static LinkedList<SandboxWaiting> _getWaitingFor(
      ConformancePersistenceProvider persistenceProvider, String sandboxId) {
    JsonNode waitingJsonNode =
        persistenceProvider.getNonLockingMap().getItemValue(SANDBOX + sandboxId, "waiting");
    LinkedList<SandboxWaiting> waitingList = new LinkedList<>();
    if (waitingJsonNode != null && waitingJsonNode.isArray()) {
      waitingJsonNode.forEach(
          objectNode -> waitingList.add(SandboxWaiting.fromJson(objectNode)));
    }
    return waitingList;
  }

  private static void _setWaitingFor(
      ConformancePersistenceProvider persistenceProvider,
      String sandboxId,
      String who,
      String forWhom,
      String toDoWhat) {
    ArrayNode waitingArrayNode = OBJECT_MAPPER.createArrayNode();
    if (who != null) {
      Stream.concat(
              _getWaitingFor(persistenceProvider, sandboxId).stream()
                  .filter(waiting -> !waiting.who().equals(who)),
              Stream.of(new SandboxWaiting(who, forWhom, toDoWhat))
                  .filter(waiting -> waiting.toDoWhat() != null))
          .forEach(waiting -> waitingArrayNode.add(waiting.toJson()));
    }
    log.info("Sandbox %s waiting: %s".formatted(sandboxId, waitingArrayNode.toPrettyString()));
    persistenceProvider
        .getNonLockingMap()
        .setItemValue(SANDBOX + sandboxId, "waiting", waitingArrayNode);
  }

  public static ObjectNode getScenarioStatus(
      ConformancePersistenceProvider persistenceProvider, String sandboxId, String scenarioId) {
    AtomicReference<ObjectNode> resultReference = new AtomicReference<>();
    new OrchestratorTask(
            persistenceProvider,
            null,
            sandboxId,
            "getting from sandbox %s the status of scenario %s".formatted(sandboxId, scenarioId),
            orchestrator -> resultReference.set(orchestrator.getScenarioStatus(scenarioId)))
        .run();
    return resultReference.get();
  }

  public static ObjectNode handleActionInput(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      String actionId,
      JsonNode actionInputOrNull) {
    ObjectNode partyInput = OBJECT_MAPPER.createObjectNode().put("actionId", actionId);
    if (actionInputOrNull != null) {
      partyInput.set("input", actionInputOrNull);
    }
    new OrchestratorTask(
            persistenceProvider,
            conformanceWebRequest ->
                ConformanceSandbox._asyncSendOutboundWebRequest(
                    deferredSandboxTaskConsumer, conformanceWebRequest),
            sandboxId,
            "handling in sandbox %s the input for action %s".formatted(sandboxId, actionId),
            orchestrator -> orchestrator.handlePartyInput(partyInput))
        .run();
    return OBJECT_MAPPER.createObjectNode();
  }

  public static void startOrStopScenario(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      String scenarioId) {
    _setWaitingFor(persistenceProvider, sandboxId, null, null, null);
    new OrchestratorTask(
            persistenceProvider,
            conformanceWebRequest ->
                ConformanceSandbox._asyncSendOutboundWebRequest(
                    deferredSandboxTaskConsumer, conformanceWebRequest),
            sandboxId,
            "starting or stopping in sandbox %s scenario %s".formatted(sandboxId, scenarioId),
            orchestrator -> orchestrator.startOrStopScenario(scenarioId))
        .run();
  }

  public static JsonNode completeCurrentAction(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      boolean skipAction) {
    new OrchestratorTask(
            persistenceProvider,
            conformanceWebRequest ->
                ConformanceSandbox._asyncSendOutboundWebRequest(
                    deferredSandboxTaskConsumer, conformanceWebRequest),
            sandboxId,
            skipAction
                ? "skipping current action in sandbox %s".formatted(sandboxId)
                : "completing current action in sandbox %s".formatted(sandboxId),
            conformanceOrchestrator -> conformanceOrchestrator.completeCurrentAction(skipAction))
        .run();
    return OBJECT_MAPPER.createObjectNode();
  }

  public static ObjectNode getCurrentActionExchanges(
      ConformancePersistenceProvider persistenceProvider, String sandboxId, String scenarioId) {
    AtomicReference<ObjectNode> resultReference = new AtomicReference<>();
    new OrchestratorTask(
            persistenceProvider,
            null,
            sandboxId,
            "getting from sandbox %s and scenario %s the current action exchanges"
                .formatted(sandboxId, scenarioId),
            orchestrator -> resultReference.set(orchestrator.getCurrentActionExchanges(scenarioId)))
        .run();
    return resultReference.get();
  }

  private static ConformanceWebResponse _handlePartyNotification(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      String partyName) {
    new PartyTask(
            persistenceProvider,
            deferredSandboxTaskConsumer,
            sandboxId,
            partyName,
            "handling notification for party " + partyName,
            ConformanceParty::handleNotification)
        .run();
    return new ConformanceWebResponse(200, JsonToolkit.JSON_UTF_8, Collections.emptyMap(), "{}");
  }

  private static ConformanceWebResponse _handlePartyInboundConformanceRequest(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      String partyName,
      ConformanceWebRequest webRequest) {
    AtomicReference<ConformanceRequest> conformanceRequestReference = new AtomicReference<>();
    AtomicReference<ConformanceResponse> conformanceResponseReference = new AtomicReference<>();
    new PartyTask(
            persistenceProvider,
            deferredSandboxTaskConsumer,
            sandboxId,
            partyName,
            "get prompt for party " + partyName,
            party -> {
              ConformanceRequest conformanceRequest =
                  new ConformanceRequest(
                      webRequest.method(),
                      webRequest.url(),
                      webRequest.queryParameters(),
                      new ConformanceMessage(
                          party.getCounterpartName(),
                          party.getCounterpartRole(),
                          party.getName(),
                          party.getRole(),
                          webRequest.headers(),
                          new ConformanceMessageBody(webRequest.body()),
                          System.currentTimeMillis()));
              conformanceRequestReference.set(conformanceRequest);
              conformanceResponseReference.set(party.handleRequest(conformanceRequest));
            })
        .run();
    ConformanceResponse conformanceResponse = conformanceResponseReference.get();
    SandboxConfiguration sandboxConfiguration =
        loadSandboxConfiguration(persistenceProvider, sandboxId);
    if (sandboxConfiguration.getOrchestrator().isActive()) {
      new OrchestratorTask(
              persistenceProvider,
              conformanceWebRequest ->
                  ConformanceSandbox._asyncSendOutboundWebRequest(
                      deferredSandboxTaskConsumer, conformanceWebRequest),
              sandboxId,
              "handling inbound conformance request",
              orchestrator ->
                  orchestrator.handlePartyTrafficExchange(
                      new ConformanceExchange(
                          conformanceRequestReference.get(), conformanceResponse)))
          .run();
    }
    return new ConformanceWebResponse(
        conformanceResponse.statusCode(),
        JsonToolkit.JSON_UTF_8,
        conformanceResponse.message().headers(),
        conformanceResponse.message().body().getStringBody());
  }

  private static void _asyncHandleOutboundRequest(
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      ConformanceRequest conformanceRequest) {
    JsonNode deferredTask =
        OBJECT_MAPPER
            .createObjectNode()
            .put("handler", "_syncHandleOutboundRequest")
            .put("sandboxId", sandboxId)
            .set("conformanceRequest", conformanceRequest.toJson());
    log.debug("Deferring task: {}", deferredTask.toPrettyString());
    deferredSandboxTaskConsumer.accept(deferredTask);
  }

  private static void _asyncSendOutboundWebRequest(
      Consumer<JsonNode> deferredSandboxTaskConsumer, ConformanceWebRequest conformanceWebRequest) {
    JsonNode deferredTask =
        OBJECT_MAPPER
            .createObjectNode()
            .put("handler", "_syncSendOutboundWebRequest")
            .set("conformanceWebRequest", conformanceWebRequest.toJson());
    log.debug("Deferring task: {}", deferredTask.toPrettyString());
    deferredSandboxTaskConsumer.accept(deferredTask);
  }

  public static void executeDeferredTask(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      JsonNode jsonNode) {
    try {
      log.debug("ConformanceSandbox.executeDeferredTask({})", jsonNode.toPrettyString());
      switch (jsonNode.path("handler").asText()) {
        case "_syncHandleOutboundRequest":
          _syncHandleOutboundRequest(
              persistenceProvider,
              deferredSandboxTaskConsumer,
              jsonNode.path("sandboxId").asText(),
              ConformanceRequest.fromJson((ObjectNode) jsonNode.get("conformanceRequest")));
          return;
        case "_syncSendOutboundWebRequest":
          _syncSendOutboundWebRequest(
              ConformanceWebRequest.fromJson((ObjectNode) jsonNode.get("conformanceWebRequest")));
          return;
        default:
          log.error("Unsupported deferred task: {}", jsonNode.toPrettyString());
      }
    } catch (Exception e) {
      log.error(
          "Deferred task execution failed: %s"
              .formatted(jsonNode == null ? null : jsonNode.toPrettyString()),
          e);
    }
  }

  @SneakyThrows
  private static ConformanceResponse _syncHttpRequest(ConformanceRequest conformanceRequest) {
    URI uri = conformanceRequest.toURI();
    log.info(
        "ConformanceSandbox.syncHttpRequest({}) request: {}",
        uri,
        conformanceRequest.toJson().toPrettyString());

    HttpRequest.Builder httpRequestBuilder =
        "GET".equals(conformanceRequest.method())
            ? HttpRequest.newBuilder().uri(uri).GET()
            : HttpRequest.newBuilder()
                .uri(uri)
                .method(
                    conformanceRequest.method(),
                    HttpRequest.BodyPublishers.ofString(
                        conformanceRequest.message().body().getStringBody()));

    // Allow long debugging sessions or slow business logic at customer's side
    httpRequestBuilder.timeout(Duration.ofHours(1));

    conformanceRequest
        .message()
        .headers()
        .forEach((name, values) -> values.forEach(value -> httpRequestBuilder.header(name, value)));

    HttpResponse<String> httpResponse =
        IOToolkit.HTTP_CLIENT.send(
            httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    ConformanceResponse conformanceResponse =
        conformanceRequest.createResponse(
            httpResponse.statusCode(),
            httpResponse.headers().map(),
            new ConformanceMessageBody(httpResponse.body()));
    log.info(
        "ConformanceSandbox.syncHttpRequest() response: {}",
        conformanceResponse.toJson().toPrettyString());
    return conformanceResponse;
  }

  private static ConformanceResponse _syncHandleOutboundRequest(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      ConformanceRequest conformanceRequest) {
    ConformanceResponse conformanceResponse = _syncHttpRequest(conformanceRequest);
    SandboxConfiguration sandboxConfiguration =
        loadSandboxConfiguration(persistenceProvider, sandboxId);
    if (!conformanceRequest.message().targetPartyRole().equals("orchestrator")
        && Arrays.stream(sandboxConfiguration.getParties())
            .noneMatch(
                partyConfiguration ->
                    Objects.equals(
                        partyConfiguration.getName(),
                        conformanceRequest.message().targetPartyName()))
        && sandboxConfiguration.getOrchestrator().isActive()) {
      new OrchestratorTask(
              persistenceProvider,
              conformanceWebRequest ->
                  ConformanceSandbox._asyncSendOutboundWebRequest(
                      deferredSandboxTaskConsumer, conformanceWebRequest),
              sandboxId,
              "handling outbound conformance request",
              orchestrator ->
                  orchestrator.handlePartyTrafficExchange(
                      new ConformanceExchange(conformanceRequest, conformanceResponse)))
          .run();
    }

    return conformanceResponse;
  }

  private static void _syncSendOutboundWebRequest(ConformanceWebRequest conformanceWebRequest) {
    try {
      // Allow long debugging sessions or slow business logic at customer's side
      HttpRequest.Builder httpRequestBuilder =
          HttpRequest.newBuilder()
              .uri(URI.create(conformanceWebRequest.url()))
              .timeout(Duration.ofHours(1))
              .GET();
      conformanceWebRequest
          .headers()
          .forEach(
              (name, values) -> values.forEach(value -> httpRequestBuilder.header(name, value)));
      IOToolkit.HTTP_CLIENT.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      log.error(
          "Failed to send outbound request: {}",
          conformanceWebRequest.toJson().toPrettyString(),
          e);
    }
  }

  private static ConformanceWebResponse _handleGetPartyPrompt(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      String partyName) {
    AtomicReference<JsonNode> partyPromptReference = new AtomicReference<>();
    new OrchestratorTask(
            persistenceProvider,
            conformanceWebRequest ->
                ConformanceSandbox._asyncSendOutboundWebRequest(
                    deferredSandboxTaskConsumer, conformanceWebRequest),
            sandboxId,
            "get prompt for party " + partyName,
            orchestrator -> partyPromptReference.set(orchestrator.handleGetPartyPrompt(partyName)))
        .run();
    log.info(
        "Returning prompt for party {}: {}",
        partyName,
        partyPromptReference.get().toPrettyString());
    return new ConformanceWebResponse(
        200,
        JsonToolkit.JSON_UTF_8,
        Collections.emptyMap(),
        partyPromptReference.get().toPrettyString());
  }

  private static ConformanceWebResponse _handleGetStatus(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId) {
    AtomicReference<JsonNode> statusReference = new AtomicReference<>();
    new OrchestratorTask(
            persistenceProvider,
            conformanceWebRequest ->
                ConformanceSandbox._asyncSendOutboundWebRequest(
                    deferredSandboxTaskConsumer, conformanceWebRequest),
            sandboxId,
            "get status",
            orchestrator -> statusReference.set(orchestrator.getStatus()))
        .run();
    return new ConformanceWebResponse(
        200, JsonToolkit.JSON_UTF_8, Collections.emptyMap(), statusReference.get().toString());
  }

  private static ConformanceWebResponse _handlePostPartyInput(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      String partyName,
      String input) {
    new OrchestratorTask(
            persistenceProvider,
            conformanceWebRequest ->
                ConformanceSandbox._asyncSendOutboundWebRequest(
                    deferredSandboxTaskConsumer, conformanceWebRequest),
            sandboxId,
            "handling input from party " + partyName,
            orchestrator ->
                orchestrator.handlePartyInput(new ConformanceMessageBody(input).getJsonBody()))
        .run();
    return new ConformanceWebResponse(200, JsonToolkit.JSON_UTF_8, Collections.emptyMap(), "{}");
  }

  private static ConformanceWebResponse _handleGenerateReport(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      boolean printable) {
    SandboxConfiguration sandboxConfiguration =
        loadSandboxConfiguration(persistenceProvider, sandboxId);

    Set<String> reportRoleNames =
        _createComponentFactory(sandboxConfiguration.getStandard(), sandboxConfiguration.getScenarioSuite())
            .getReportRoleNames(
                sandboxConfiguration.getParties(), sandboxConfiguration.getCounterparts());

    AtomicReference<String> reportReference = new AtomicReference<>();
    new OrchestratorTask(
            persistenceProvider,
            conformanceWebRequest ->
                ConformanceSandbox._asyncSendOutboundWebRequest(
                    deferredSandboxTaskConsumer, conformanceWebRequest),
            sandboxId,
            "generating report for roles: " + reportRoleNames,
            orchestrator ->
                reportReference.set(orchestrator.generateReport(reportRoleNames, printable)))
        .run();
    return new ConformanceWebResponse(
        200, "text/html;charset=utf-8", Collections.emptyMap(), reportReference.get());
  }

  private static ConformanceWebResponse _handleReset(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId) {
    String newSessionId = UUID.randomUUID().toString();
    persistenceProvider
        .getStatefulExecutor()
        .execute(
            "update sandbox currentSessionId",
            SANDBOX + sandboxId,
            "state",
            originalSandboxState ->
                OBJECT_MAPPER.createObjectNode().put("currentSessionId", newSessionId));
    persistenceProvider
        .getNonLockingMap()
        .setItemValue(
            SANDBOX + sandboxId,
            "SK=session#%s#%s".formatted(Instant.now().toString(), newSessionId),
            OBJECT_MAPPER.createObjectNode());

    SandboxConfiguration sandboxConfiguration =
        loadSandboxConfiguration(persistenceProvider, sandboxId);
    if (sandboxConfiguration.getOrchestrator().isActive()) {
      new OrchestratorTask(
              persistenceProvider,
              conformanceWebRequest ->
                  ConformanceSandbox._asyncSendOutboundWebRequest(
                      deferredSandboxTaskConsumer, conformanceWebRequest),
              sandboxId,
              "starting session",
              ConformanceOrchestrator::notifyNextActionParty)
          .run();
    }

    return new ConformanceWebResponse(200, JsonToolkit.JSON_UTF_8, Collections.emptyMap(), "{}");
  }

  public static SandboxConfiguration loadSandboxConfiguration(
      ConformancePersistenceProvider persistenceProvider, String sandboxId) {
    return SandboxConfiguration.fromJsonNode(
        persistenceProvider.getNonLockingMap().getItemValue(SANDBOX + sandboxId, "config"));
  }

  public static void saveSandboxConfiguration(
      ConformancePersistenceProvider persistenceProvider,
      String environmentId,
      SandboxConfiguration sandboxConfiguration) {
    persistenceProvider
        .getNonLockingMap()
        .setItemValue(
            "environment#" + environmentId,
            SANDBOX + sandboxConfiguration.getId(),
            OBJECT_MAPPER
                .createObjectNode()
                .put("id", sandboxConfiguration.getId())
                .put("name", sandboxConfiguration.getName()));
    persistenceProvider
        .getNonLockingMap()
        .setItemValue(
            SANDBOX + sandboxConfiguration.getId(), "config", sandboxConfiguration.toJsonNode());
  }

  private static AbstractComponentFactory _createComponentFactory(
      StandardConfiguration standardConfiguration, String scenarioSuite) {
    return Arrays.stream(SUPPORTED_STANDARDS)
        .filter(standard -> standard.getName().equals(standardConfiguration.getName()))
        .findFirst()
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    "Unsupported standard: %s".formatted(standardConfiguration)))
        .createComponentFactory(standardConfiguration.getVersion(), scenarioSuite);
  }

  public static JsonNode executeAdminTask(
      ConformancePersistenceProvider persistenceProvider,
      JsonNode jsonInput) {
    String operation = jsonInput.path("operation").asText();
    if ("createReportInAllSandboxes".equals(operation)) {
      String reportTitle = jsonInput.get("reportTitle").asText(); // throw NPE if missing
      return createReportInAllSandboxes(persistenceProvider, reportTitle);
    }
    throw new UnsupportedOperationException("Unsupported operation '%s'".formatted(operation));
  }

  private static JsonNode createReportInAllSandboxes(
      ConformancePersistenceProvider persistenceProvider, String reportTitle) {
    ArrayNode environmentResults = OBJECT_MAPPER.createArrayNode();
    TreeMap<String, TreeSet<String>> sandboxIdsByEnvironmentId =
        getSandboxIdsByEnvironmentId(persistenceProvider);
    int environmentCount = sandboxIdsByEnvironmentId.size();
    AtomicInteger atomicEnvironmentIndex = new AtomicInteger();
    sandboxIdsByEnvironmentId.forEach(
        (environmentId, sandboxIds) -> {
          ArrayNode sandboxResults = OBJECT_MAPPER.createArrayNode();
          int environmentIndex = atomicEnvironmentIndex.incrementAndGet();
          int sandboxCount = sandboxIds.size();
          AtomicInteger atomicSandboxIndex = new AtomicInteger();
          sandboxIds.forEach(
              sandboxId -> {
                log.info(
                    "In environment {} of {} with id {} creating report for sandbox {} of {} with id {}",
                    environmentIndex,
                    environmentCount,
                    environmentId,
                    atomicSandboxIndex.incrementAndGet(),
                    sandboxCount,
                    sandboxId);
                String sandboxResultString;
                if (sandboxId.contains("#deleted")) {
                  sandboxResultString = "Skipped (deleted sandbox)";
                } else {
                  try {
                    SandboxConfiguration sandboxConfiguration =
                        loadSandboxConfiguration(persistenceProvider, sandboxId);
                    if (sandboxConfiguration.getSandboxPartyCounterpartConfiguration() == null) {
                      sandboxResultString = "Skipped (no counterpart configuration)";
                    } else if (!sandboxConfiguration.getOrchestrator().isActive()) {
                      sandboxResultString = "Skipped (internal sandbox)";
                    } else {
                      createReport(
                          persistenceProvider, environmentId, sandboxConfiguration, reportTitle);
                      sandboxResultString = "DONE";
                    }
                  } catch (Exception e) {
                    log.warn("Sandbox report creation failed: {}", e, e);
                    sandboxResultString = "Failed: " + e;
                  }
                }
                sandboxResults.add(
                    OBJECT_MAPPER
                        .createObjectNode()
                        .put("sandboxId", sandboxId)
                        .put("result", sandboxResultString));
              });
          ObjectNode environmentResult =
              OBJECT_MAPPER.createObjectNode().put("environmentId", environmentId);
          environmentResult.set("sandboxResults", sandboxResults);
          environmentResults.add(environmentResult);
        });
    return environmentResults;
  }

  private static TreeMap<String, TreeSet<String>> getSandboxIdsByEnvironmentId(
    ConformancePersistenceProvider persistenceProvider) {
    TreeMap<String, TreeSet<String>> sandboxIdsByEnvironmentId = new TreeMap<>();
    String partitionKeyPrefix = "environment#";
    String sortKeyPrefix = "sandbox#";
    persistenceProvider
        .getNonLockingMap()
        .scan(partitionKeyPrefix, sortKeyPrefix)
        .forEach(
            (partitionKey, valuesBySortKey) -> {
              TreeSet<String> sandboxIds = new TreeSet<>();
              sandboxIdsByEnvironmentId.put(
                  partitionKey.substring(partitionKeyPrefix.length()), sandboxIds);
              valuesBySortKey
                  .keySet()
                  .forEach(sortKey -> sandboxIds.add(sortKey.substring(sortKeyPrefix.length())));
            });
    return sandboxIdsByEnvironmentId;
  }
}
