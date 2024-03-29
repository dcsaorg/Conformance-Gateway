package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.dcsa.conformance.sandbox.configuration.StandardConfiguration;
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
public class ConformanceSandbox {
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
              "session#" + currentSessionId,
              "state#orchestrator",
              originalOrchestratorState -> {
                SandboxConfiguration sandboxConfiguration =
                    loadSandboxConfiguration(persistenceProvider, sandboxId);
                AbstractComponentFactory componentFactory =
                    _createComponentFactory(sandboxConfiguration.getStandard());
                ConformanceOrchestrator orchestrator =
                    new ConformanceOrchestrator(
                        sandboxConfiguration,
                        componentFactory,
                        new TrafficRecorder(
                            persistenceProvider.getNonLockingMap(), "session#" + currentSessionId),
                        new JsonNodeMap(
                            persistenceProvider.getNonLockingMap(),
                            "session#" + currentSessionId,
                            "map#orchestrator#"),
                        asyncWebClient);
                if (originalOrchestratorState != null && !originalOrchestratorState.isEmpty()) {
                  orchestrator.importJsonState(originalOrchestratorState);
                }
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
              "session#" + currentSessionId,
              "state#party#" + partyName,
              originalPartyState -> {
                SandboxConfiguration sandboxConfiguration =
                    loadSandboxConfiguration(persistenceProvider, sandboxId);
                AbstractComponentFactory componentFactory =
                    _createComponentFactory(sandboxConfiguration.getStandard());

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
                                "session#" + currentSessionId,
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
            "sandbox#" + sandboxId,
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
        "ConformanceSandbox.handleRequest() "
            + OBJECT_MAPPER.valueToTree(webRequest).toPrettyString());

    String expectedPrefix = "/conformance/sandbox/";
    int expectedPrefixStart = webRequest.url().indexOf(expectedPrefix);
    if (expectedPrefixStart < 0)
      throw new IllegalArgumentException(
          "Missing '%s' in: %s".formatted(expectedPrefix, webRequest.url()));
    String remainingUri = webRequest.url().substring(expectedPrefixStart + expectedPrefix.length());

    int endOfSandboxId = remainingUri.indexOf("/");
    if (endOfSandboxId < 0) {
      throw new IllegalArgumentException("Missing sandbox id: " + webRequest.url());
    }
    String sandboxId = remainingUri.substring(0, endOfSandboxId);
    remainingUri = remainingUri.substring(endOfSandboxId);

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
      String partyName = remainingUri.substring(0, endOfPartyName);
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
            party -> party.getOperatorLog().forEach(operatorLogNode::add))
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
    if (sandboxConfiguration.getOrchestrator().isActive()) return;

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

  public static JsonNode startOrStopScenario(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      String sandboxId,
      String scenarioId) {
    new OrchestratorTask(
            persistenceProvider,
            conformanceWebRequest ->
                ConformanceSandbox._asyncSendOutboundWebRequest(
                    deferredSandboxTaskConsumer, conformanceWebRequest),
            sandboxId,
            "starting in sandbox %s scenario %s".formatted(sandboxId, scenarioId),
            orchestrator -> orchestrator.startOrStopScenario(scenarioId))
        .run();
    return OBJECT_MAPPER.createObjectNode();
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
    log.debug("Deferring task: " + deferredTask.toPrettyString());
    deferredSandboxTaskConsumer.accept(deferredTask);
  }

  private static void _asyncSendOutboundWebRequest(
      Consumer<JsonNode> deferredSandboxTaskConsumer, ConformanceWebRequest conformanceWebRequest) {
    JsonNode deferredTask =
        OBJECT_MAPPER
            .createObjectNode()
            .put("handler", "_syncSendOutboundWebRequest")
            .set("conformanceWebRequest", conformanceWebRequest.toJson());
    log.debug("Deferring task: " + deferredTask.toPrettyString());
    deferredSandboxTaskConsumer.accept(deferredTask);
  }

  public static void executeDeferredTask(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<JsonNode> deferredSandboxTaskConsumer,
      JsonNode jsonNode) {
    try {
      log.debug("ConformanceSandbox.executeDeferredTask(%s)".formatted(jsonNode.toPrettyString()));
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
          log.error("Unsupported deferred task: " + jsonNode.toPrettyString());
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
        "ConformanceSandbox.syncHttpRequest(%s) request: %s"
            .formatted(uri, conformanceRequest.toJson().toPrettyString()));

    HttpRequest.Builder httpRequestBuilder =
        "GET".equals(conformanceRequest.method())
            ? HttpRequest.newBuilder().uri(uri).GET()
            : HttpRequest.newBuilder()
                .uri(uri)
                .method(
                    conformanceRequest.method(),
                    HttpRequest.BodyPublishers.ofString(
                        conformanceRequest.message().body().getStringBody()));

    httpRequestBuilder.timeout(Duration.ofHours(1));

    conformanceRequest
        .message()
        .headers()
        .forEach((name, values) -> values.forEach(value -> httpRequestBuilder.header(name, value)));

    HttpResponse<String> httpResponse;
    try (HttpClient httpClient =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
      httpResponse =
          httpClient.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }
    ConformanceResponse conformanceResponse =
        conformanceRequest.createResponse(
            httpResponse.statusCode(),
            httpResponse.headers().map(),
            new ConformanceMessageBody(httpResponse.body()));
    log.info(
        "ConformanceSandbox.syncHttpRequest() response: %s"
            .formatted(conformanceResponse.toJson().toPrettyString()));
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
                        conformanceRequest.message().targetPartyName()))) {
      if (sandboxConfiguration.getOrchestrator().isActive()) {
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
    }
    return conformanceResponse;
  }

  private static void _syncSendOutboundWebRequest(ConformanceWebRequest conformanceWebRequest) {
    try {
      HttpRequest.Builder httpRequestBuilder =
          HttpRequest.newBuilder()
              .uri(URI.create(conformanceWebRequest.url()))
              .timeout(Duration.ofHours(1))
              .GET();
      conformanceWebRequest
          .headers()
          .forEach(
              (name, values) -> values.forEach(value -> httpRequestBuilder.header(name, value)));
      try (HttpClient httpClient = HttpClient.newHttpClient()) {
        httpClient.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
      }
    } catch (Exception e) {
      log.error(
          "Failed to send outbound request: %s"
              .formatted(conformanceWebRequest.toJson().toPrettyString()),
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
        "Returning prompt for party %s: %s"
            .formatted(partyName, partyPromptReference.get().toPrettyString()));
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
        _createComponentFactory(sandboxConfiguration.getStandard())
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
            "sandbox#" + sandboxId,
            "state",
            originalSandboxState ->
                OBJECT_MAPPER.createObjectNode().put("currentSessionId", newSessionId));
    persistenceProvider
        .getNonLockingMap()
        .setItemValue(
            "sandbox#" + sandboxId,
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
        persistenceProvider.getNonLockingMap().getItemValue("sandbox#" + sandboxId, "config"));
  }

  public static void saveSandboxConfiguration(
      ConformancePersistenceProvider persistenceProvider,
      String environmentId,
      SandboxConfiguration sandboxConfiguration) {
    persistenceProvider
        .getNonLockingMap()
        .setItemValue(
            "environment#" + environmentId,
            "sandbox#" + sandboxConfiguration.getId(),
            OBJECT_MAPPER
                .createObjectNode()
                .put("id", sandboxConfiguration.getId())
                .put("name", sandboxConfiguration.getName()));
    persistenceProvider
        .getNonLockingMap()
        .setItemValue(
            "sandbox#" + sandboxConfiguration.getId(), "config", sandboxConfiguration.toJsonNode());
  }

  private static AbstractComponentFactory _createComponentFactory(
      StandardConfiguration standardConfiguration) {
    if (BookingComponentFactory.STANDARD_NAME.equals(standardConfiguration.getName())) {
      return new BookingComponentFactory(standardConfiguration.getVersion());
    }
    if (EblComponentFactory.STANDARD_NAME.equals(standardConfiguration.getName())) {
      return new EblComponentFactory(standardConfiguration.getVersion());
    }
    if (EblIssuanceComponentFactory.STANDARD_NAME.equals(standardConfiguration.getName())) {
      return new EblIssuanceComponentFactory(standardConfiguration.getVersion());
    }
    if (EblSurrenderComponentFactory.STANDARD_NAME.equals(standardConfiguration.getName())) {
      return new EblSurrenderComponentFactory(standardConfiguration.getVersion());
    }
    if (JitComponentFactory.STANDARD_NAME.equals(standardConfiguration.getName())) {
      return new JitComponentFactory(standardConfiguration.getVersion());
    }
    if (OvsComponentFactory.STANDARD_NAME.equals(standardConfiguration.getName())) {
      return new OvsComponentFactory(standardConfiguration.getVersion());
    }
    if (PintComponentFactory.STANDARD_NAME.equals(standardConfiguration.getName())) {
      return new PintComponentFactory(standardConfiguration.getVersion());
    }
    if (TntComponentFactory.STANDARD_NAME.equals(standardConfiguration.getName())) {
      return new TntComponentFactory(standardConfiguration.getVersion());
    }
    throw new UnsupportedOperationException(
        "Unsupported standard: %s".formatted(standardConfiguration));
  }
}
