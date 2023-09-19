package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.ComponentFactory;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.dcsa.conformance.sandbox.configuration.StandardConfiguration;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10ComponentFactory;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10Role;

@Slf4j
public class ConformanceSandbox {
  public record OrchestratorTask(
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
                    _loadSandboxConfiguration(persistenceProvider, sandboxId);
                ComponentFactory componentFactory =
                    _createComponentFactory(sandboxConfiguration.getStandard());
                ConformanceOrchestrator orchestrator =
                    new ConformanceOrchestrator(
                        sandboxConfiguration,
                        componentFactory,
                        _createTrafficRecorder(persistenceProvider, "session#" + currentSessionId),
                        asyncWebClient);
                if (!originalOrchestratorState.isEmpty()) {
                  orchestrator.importJsonState(originalOrchestratorState);
                }
                orchestratorConsumer.accept(orchestrator);
                return orchestrator.exportJsonState();
              });
    }
  }

  public record PartyTask(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      String sandboxId,
      String partyName,
      String description,
      Consumer<ConformanceParty> partyConsumer)
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
              "state#party#" + partyName,
              originalPartyState -> {
                SandboxConfiguration sandboxConfiguration =
                    _loadSandboxConfiguration(persistenceProvider, sandboxId);
                ComponentFactory componentFactory =
                    _createComponentFactory(sandboxConfiguration.getStandard());
                ConformanceParty party =
                    componentFactory
                        .createParties(
                            sandboxConfiguration.getParties(),
                            sandboxConfiguration.getCounterparts(),
                            conformanceRequest ->
                                _handlePartyOutboundConformanceRequest(
                                    persistenceProvider,
                                    asyncWebClient,
                                    sandboxId,
                                    conformanceRequest))
                        .stream()
                        .filter(createdParty -> partyName.equals(createdParty.getName()))
                        .findFirst()
                        .orElseThrow(
                            () -> new IllegalArgumentException("Party not found: " + partyName));
                if (!originalPartyState.isEmpty()) {
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
            "load sandbox state",
            "sandbox#" + sandboxId,
            "state",
            originalSandboxState -> {
              sandboxStateNodeReference.set(originalSandboxState);
              return null;
            });
    return sandboxStateNodeReference.get();
  }

  public static final String JSON_UTF_8 = "application/json;charset=utf-8";

  public static void create(
      ConformancePersistenceProvider persistenceProvider,
      String sandboxId,
      SandboxConfiguration sandboxConfiguration) {
    persistenceProvider
        .getNonLockingMap()
        .setItemValue("sandbox#" + sandboxId, "config", sandboxConfiguration.toJsonNode());
  }

  public static ConformanceWebResponse handleRequest(
      ConformancePersistenceProvider persistenceProvider,
      ConformanceWebRequest webRequest,
      Consumer<ConformanceWebRequest> asyncWebClient) {
    String expectedPrefix = "/conformance/sandbox/";
    if (!webRequest.uri().startsWith(expectedPrefix)) {
      throw new IllegalArgumentException("Incorrect URI prefix: " + webRequest.uri());
    }
    String remainingUri = webRequest.uri().substring(expectedPrefix.length());

    int endOfSandboxId = remainingUri.indexOf("/");
    if (endOfSandboxId < 0) {
      throw new IllegalArgumentException("Missing sandbox id: " + webRequest.uri());
    }
    String sandboxId = remainingUri.substring(0, endOfSandboxId);
    remainingUri = remainingUri.substring(endOfSandboxId);

    if (remainingUri.startsWith("/party/")) {
      remainingUri = remainingUri.substring("/party/".length());
      int endOfPartyName = remainingUri.indexOf("/");
      if (endOfPartyName < 0) {
        throw new IllegalArgumentException("Missing party name: " + webRequest.uri());
      }
      String partyName = remainingUri.substring(0, endOfPartyName);
      remainingUri = remainingUri.substring(endOfPartyName);

      if (remainingUri.equals("/notification")) {
        return _handlePartyNotification(persistenceProvider, asyncWebClient, sandboxId, partyName);
      } else if (remainingUri.equals("/prompt/json")) {
        return _handleGetPartyPrompt(persistenceProvider, asyncWebClient, sandboxId, partyName);
      } else if (remainingUri.equals("/input")) {
        return _handlePostPartyInput(
            persistenceProvider, asyncWebClient, sandboxId, partyName, webRequest.body());
      } else if (remainingUri.startsWith("/api")) {
        return _handlePartyInboundConformanceRequest(
            persistenceProvider, asyncWebClient, sandboxId, partyName, webRequest);
      }
    } else if (remainingUri.equals("/status")) {
      return _handleGetStatus(persistenceProvider, asyncWebClient, sandboxId);
    } else if (remainingUri.equals("/report")) {
      return _handleGenerateReport(persistenceProvider, asyncWebClient, sandboxId);
    } else if (remainingUri.equals("/reset")) {
      return _handleReset(persistenceProvider, asyncWebClient, sandboxId);
    }
    throw new IllegalArgumentException("Unhandled URI: " + webRequest.uri());
  }

  private static ConformanceWebResponse _handlePartyNotification(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      String sandboxId,
      String partyName) {
    new PartyTask(
            persistenceProvider,
            asyncWebClient,
            sandboxId,
            partyName,
            "handling notification for party " + partyName,
            ConformanceParty::handleNotification)
        .run();
    return new ConformanceWebResponse(200, JSON_UTF_8, Collections.emptyMap(), "{}");
  }

  private static ConformanceWebResponse _handlePartyInboundConformanceRequest(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      String sandboxId,
      String partyName,
      ConformanceWebRequest webRequest) {
    AtomicReference<ConformanceRequest> conformanceRequestReference = new AtomicReference<>();
    AtomicReference<ConformanceResponse> conformanceResponseReference = new AtomicReference<>();
    new PartyTask(
            persistenceProvider,
            asyncWebClient,
            sandboxId,
            partyName,
            "get prompt for party " + partyName,
            party -> {
              ConformanceRequest conformanceRequest =
                  new ConformanceRequest(
                      webRequest.method(),
                      webRequest.baseUrl(),
                      webRequest.uri(),
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
        _loadSandboxConfiguration(persistenceProvider, sandboxId);
    if (sandboxConfiguration.getOrchestrator() != null) {
      new OrchestratorTask(
              persistenceProvider,
              asyncWebClient,
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
        JSON_UTF_8,
        conformanceResponse.message().headers(),
        conformanceResponse.message().body().getStringBody());
  }

  private static void _handlePartyOutboundConformanceRequest(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      String sandboxId,
      ConformanceRequest conformanceRequest) {
    ConformanceResponse conformanceResponse = _syncHttpRequest(conformanceRequest);
    SandboxConfiguration sandboxConfiguration =
        _loadSandboxConfiguration(persistenceProvider, sandboxId);
    if (!conformanceRequest.message().targetPartyRole().equals("orchestrator")
        && Arrays.stream(sandboxConfiguration.getParties())
            .noneMatch(
                partyConfiguration ->
                    Objects.equals(
                        partyConfiguration.getName(),
                        conformanceRequest.message().targetPartyName()))) {
      if (sandboxConfiguration.getOrchestrator() != null) {
        new OrchestratorTask(
                persistenceProvider,
                asyncWebClient,
                sandboxId,
                "handling outbound conformance request",
                orchestrator ->
                    orchestrator.handlePartyTrafficExchange(
                        new ConformanceExchange(conformanceRequest, conformanceResponse)))
            .run();
      }
    }
  }

  private static ConformanceWebResponse _handleGetPartyPrompt(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      String sandboxId,
      String partyName) {
    AtomicReference<JsonNode> partyPromptReference = new AtomicReference<>();
    new OrchestratorTask(
            persistenceProvider,
            asyncWebClient,
            sandboxId,
            "get prompt for party " + partyName,
            orchestrator -> {
              partyPromptReference.set(orchestrator.handleGetPartyPrompt(partyName));
            })
        .run();
    log.info(
        "Returning prompt for party %s: %s"
            .formatted(partyName, partyPromptReference.get().toPrettyString()));
    return new ConformanceWebResponse(
        200, JSON_UTF_8, Collections.emptyMap(), partyPromptReference.get().toPrettyString());
  }

  private static ConformanceWebResponse _handleGetStatus(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      String sandboxId) {
    AtomicReference<JsonNode> statusReference = new AtomicReference<>();
    new OrchestratorTask(
            persistenceProvider,
            asyncWebClient,
            sandboxId,
            "get status",
            orchestrator -> {
              statusReference.set(orchestrator.getStatus());
            })
        .run();
    return new ConformanceWebResponse(
        200, JSON_UTF_8, Collections.emptyMap(), statusReference.get().toString());
  }

  private static ConformanceWebResponse _handlePostPartyInput(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      String sandboxId,
      String partyName,
      String input) {
    new OrchestratorTask(
            persistenceProvider,
            asyncWebClient,
            sandboxId,
            "handling input from party " + partyName,
            orchestrator -> {
              orchestrator.handlePartyInput(new ConformanceMessageBody(input).getJsonBody());
            })
        .run();
    return new ConformanceWebResponse(200, JSON_UTF_8, Collections.emptyMap(), "{}");
  }

  private static ConformanceWebResponse _handleGenerateReport(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      String sandboxId) {
    SandboxConfiguration sandboxConfiguration =
        _loadSandboxConfiguration(persistenceProvider, sandboxId);

    Set<String> reportRoleNames =
        (sandboxConfiguration.getParties().length == EblSurrenderV10Role.values().length
                ? Arrays.stream(EblSurrenderV10Role.values())
                    .map(EblSurrenderV10Role::getConfigName)
                : Arrays.stream(sandboxConfiguration.getCounterparts())
                    .map(CounterpartConfiguration::getRole)
                    .filter(
                        counterpartRole ->
                            Arrays.stream(sandboxConfiguration.getParties())
                                .map(PartyConfiguration::getRole)
                                .noneMatch(
                                    partyRole -> Objects.equals(partyRole, counterpartRole))))
            .collect(Collectors.toSet());

    AtomicReference<String> reportReference = new AtomicReference<>();
    new OrchestratorTask(
            persistenceProvider,
            asyncWebClient,
            sandboxId,
            "generating report for roles: " + reportRoleNames,
            orchestrator -> {
              reportReference.set(orchestrator.generateReport(reportRoleNames));
            })
        .run();
    return new ConformanceWebResponse(
        200, "text/html;charset=utf-8", Collections.emptyMap(), reportReference.get());
  }

  private static ConformanceWebResponse _handleReset(
      ConformancePersistenceProvider persistenceProvider,
      Consumer<ConformanceWebRequest> asyncWebClient,
      String sandboxId) {
    String newSessionId = UUID.randomUUID().toString();
    persistenceProvider
        .getStatefulExecutor()
        .execute(
            "update sandbox currentSessionId",
            "sandbox#" + sandboxId,
            "state",
            originalSandboxState ->
                new ObjectMapper().createObjectNode().put("currentSessionId", newSessionId));
    persistenceProvider
        .getNonLockingMap()
        .setItemValue(
            "sandbox#" + sandboxId,
            "SK=session#%s#%s".formatted(Instant.now().toString(), newSessionId),
            new ObjectMapper().createObjectNode());

    SandboxConfiguration sandboxConfiguration =
        _loadSandboxConfiguration(persistenceProvider, sandboxId);
    if (sandboxConfiguration.getOrchestrator() != null) {
      new OrchestratorTask(
              persistenceProvider,
              asyncWebClient,
              sandboxId,
              "starting session",
              ConformanceOrchestrator::notifyRelevantParties)
          .run();
    }

    return new ConformanceWebResponse(200, JSON_UTF_8, Collections.emptyMap(), "{}");
  }

  private static SandboxConfiguration _loadSandboxConfiguration(
      ConformancePersistenceProvider persistenceProvider, String sandboxId) {
    return SandboxConfiguration.fromJsonNode(
        persistenceProvider.getNonLockingMap().getItemValue("sandbox#" + sandboxId, "config"));
  }

  private static ComponentFactory _createComponentFactory(
      StandardConfiguration standardConfiguration) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {
      return new EblSurrenderV10ComponentFactory();
    }
    throw new UnsupportedOperationException(
        "Unsupported standard: %s".formatted(standardConfiguration));
  }

  private static TrafficRecorder _createTrafficRecorder(
      ConformancePersistenceProvider persistenceProvider, String sessionId) {
    return new TrafficRecorder(persistenceProvider.getNonLockingMap(), "session#" + sessionId);
  }

  @SneakyThrows
  private static ConformanceResponse _syncHttpRequest(ConformanceRequest conformanceRequest) {
    URI uri = URI.create(conformanceRequest.baseUrl() + conformanceRequest.path());
    log.info(
        "ConformanceSandbox.syncHttpRequest(%s) calling: %s"
            .formatted(conformanceRequest.toJson().toPrettyString(), uri));
    HttpRequest.Builder httpRequestBuilder =
        HttpRequest.newBuilder()
            .uri(uri)
            .method(
                conformanceRequest.method(),
                HttpRequest.BodyPublishers.ofString(
                    conformanceRequest.message().body().getStringBody()))
            .timeout(Duration.ofHours(1));
    conformanceRequest
        .message()
        .headers()
        .forEach((name, values) -> values.forEach(value -> httpRequestBuilder.header(name, value)));
    HttpResponse<String> httpResponse =
        HttpClient.newHttpClient()
            .send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    return conformanceRequest.createResponse(
        httpResponse.statusCode(),
        httpResponse.headers().map(),
        new ConformanceMessageBody(httpResponse.body()));
  }
}
