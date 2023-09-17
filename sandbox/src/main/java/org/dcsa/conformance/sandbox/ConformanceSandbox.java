package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.ComponentFactory;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.dcsa.conformance.sandbox.configuration.StandardConfiguration;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10ComponentFactory;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10Role;

@Slf4j
public class ConformanceSandbox {
  public static final String JSON_UTF_8 = "application/json;charset=utf-8";

  public static ConformanceSandbox create(
      ConformancePersistenceProvider persistenceProvider,
      String sandboxId,
      SandboxConfiguration sandboxConfiguration) {
    AtomicReference<ConformanceSandbox> sandboxReference = new AtomicReference<>();
    persistenceProvider
        .getStatefulExecutor()
        .execute(
            "ConformanceSandbox[%s]::create()".formatted(sandboxId),
            "sandbox#%s".formatted(sandboxId),
            "state",
            ignoredOriginalState -> {
              ConformanceSandbox sandbox =
                  new ConformanceSandbox(persistenceProvider, sandboxId, sandboxConfiguration);
              sandboxReference.set(sandbox);
              return sandbox._exportJsonState();
            });
    return sandboxReference.get();
  }

  public static ConformanceWebResponse handleRequest(
      ConformancePersistenceProvider persistenceProvider,
      ConformanceWebRequest conformanceWebRequest) {
    String sandboxId = _getRequestSandboxId(conformanceWebRequest);

    AtomicReference<ConformanceWebResponse> responseReference = new AtomicReference<>();
    persistenceProvider
        .getStatefulExecutor()
        .execute(
            "ConformanceSandbox[%s]::handleRequest(%s)"
                .formatted(sandboxId, conformanceWebRequest.uri()),
            "sandbox#%s".formatted(sandboxId),
            "state",
            originalState -> {
              ConformanceSandbox sandbox =
                  new ConformanceSandbox(persistenceProvider, sandboxId, originalState);
              responseReference.set(sandbox._handleRequest(conformanceWebRequest));
              return sandbox._exportJsonState();
            });
    return responseReference.get();
  }

  private static String _getRequestSandboxId(ConformanceWebRequest conformanceWebRequest) {
    String expectedPrefix = "/conformance/sandbox/";
    String requestUri = conformanceWebRequest.uri();
    if (!requestUri.startsWith(expectedPrefix)) {
      throw new UnsupportedOperationException(
          "URI does not start with '%s': %s".formatted(expectedPrefix, requestUri));
    }
    String prefixStrippedUri = requestUri.substring(expectedPrefix.length());
    int endOfSandboxId = prefixStrippedUri.indexOf("/");
    if (endOfSandboxId < 0) {
      throw new UnsupportedOperationException(
          "Sandbox id not found in URI: %s".formatted(requestUri));
    }
    return prefixStrippedUri.substring(0, endOfSandboxId);
  }

  private final ConformancePersistenceProvider persistenceProvider;

  @Getter private final String id;
  private final SandboxConfiguration sandboxConfiguration;
  private UUID currentSessionId;
  private ConformanceOrchestrator conformanceOrchestrator;

  private List<ConformanceParty> parties;

  private ConformanceSandbox(
      ConformancePersistenceProvider persistenceProvider,
      String id,
      SandboxConfiguration sandboxConfiguration) {
    this.persistenceProvider = persistenceProvider;
    this.id = id;
    this.sandboxConfiguration = sandboxConfiguration;
    _reset(false);
  }

  private void _reset(boolean notifyParties) {
    currentSessionId = UUID.randomUUID();
    ComponentFactory componentFactory = _createComponentFactory(sandboxConfiguration.getStandard());
    if (sandboxConfiguration.getOrchestrator() != null) {
      this.conformanceOrchestrator =
          new ConformanceOrchestrator(
              sandboxConfiguration,
              componentFactory,
              this::asyncOrchestratorAction,
              new TrafficRecorder(
                  persistenceProvider.getNonLockingMap(),
                  "session#%s#traffic".formatted("TODO_currentSessionId"))); // TODO
    }
    this.parties =
        componentFactory.createParties(
            sandboxConfiguration.getParties(),
            sandboxConfiguration.getCounterparts(),
            this::asyncOutboundRequest,
            this::asyncPartyAction);
    if (notifyParties && this.conformanceOrchestrator != null) {
      this.conformanceOrchestrator.scheduleNotifyAllParties();
    }
  }

  private ConformanceSandbox(
      ConformancePersistenceProvider persistenceProvider, String id, JsonNode jsonState) {
    this(
        persistenceProvider,
        id,
        SandboxConfiguration.fromJsonNode(jsonState.get("sandboxConfiguration")));
    JsonNode currentSessionIdNode = jsonState.get("currentSessionId");
    currentSessionId =
        currentSessionIdNode == null ? null : UUID.fromString(currentSessionIdNode.asText());
    if (conformanceOrchestrator != null) {
      conformanceOrchestrator.importJsonState(jsonState.get("conformanceOrchestrator"));
    }
    parties.forEach(
        party -> party.importJsonState(jsonState.get("partiesByName").get(party.getName())));
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

  private JsonNode _exportJsonState() {
    ObjectNode jsonState = new ObjectMapper().createObjectNode();

    jsonState.put("currentSessionId", currentSessionId.toString());

    jsonState.set("sandboxConfiguration", sandboxConfiguration.toJsonNode());
    if (conformanceOrchestrator != null) {
      jsonState.set("conformanceOrchestrator", conformanceOrchestrator.exportJsonState());
    }

    ObjectNode jsonPartiesByName = new ObjectMapper().createObjectNode();
    parties.forEach(party -> jsonPartiesByName.set(party.getName(), party.exportJsonState()));
    jsonState.set("partiesByName", jsonPartiesByName);

    return jsonState;
  }

  private void asyncOrchestratorAction(Consumer<ConformanceOrchestrator> orchestratorAction) {
    CompletableFuture.runAsync(
            () -> {
              log.info("ConformanceSandbox.asyncOrchestratorAction()");
              persistenceProvider
                  .getStatefulExecutor()
                  .execute(
                      "ConformanceSandbox[%s]::asyncOrchestratorAction()".formatted(id),
                      "sandbox#%s".formatted(id),
                      "state",
                      originalState -> {
                        ConformanceSandbox sandbox =
                            new ConformanceSandbox(persistenceProvider, id, originalState);
                        orchestratorAction.accept(sandbox.conformanceOrchestrator);
                        return sandbox._exportJsonState();
                      });
            })
        .exceptionally(
            e -> {
              log.error("ConformanceSandbox.asyncOrchestratorAction() failed: %s".formatted(e), e);
              return null;
            });
  }

  private void asyncPartyAction(String partyName, Consumer<ConformanceParty> partyAction) {
    CompletableFuture.runAsync(
            () -> {
              log.info("ConformanceSandbox.asyncPartyAction(%s)".formatted(partyName));
              persistenceProvider
                  .getStatefulExecutor()
                  .execute(
                      "ConformanceSandbox[%s]::asyncPartyAction(%s)".formatted(id, partyName),
                      "sandbox#%s".formatted(id),
                      "state",
                      originalState -> {
                        ConformanceSandbox sandbox =
                            new ConformanceSandbox(persistenceProvider, id, originalState);
                        partyAction.accept(
                            sandbox.parties.stream()
                                .filter(party -> partyName.equals(party.getName()))
                                .findFirst()
                                .orElseThrow());
                        return sandbox._exportJsonState();
                      });
            })
        .exceptionally(
            e -> {
              log.error(
                  "ConformanceSandbox.asyncPartyAction(%s) failed: %s".formatted(partyName, e), e);
              return null;
            });
  }

  private void asyncOutboundRequest(ConformanceRequest conformanceRequest) {
    CompletableFuture.runAsync(
            () -> {
              log.info("ConformanceSandbox.asyncOutboundRequest(%s)".formatted(conformanceRequest));

              ConformanceResponse conformanceResponse = syncHttpRequest(conformanceRequest);

              if (!conformanceRequest.message().targetPartyRole().equals("orchestrator")
                  && Arrays.stream(sandboxConfiguration.getParties())
                      .noneMatch(
                          partyConfiguration ->
                              Objects.equals(
                                  partyConfiguration.getName(),
                                  conformanceRequest.message().targetPartyName()))) {
                log.info(
                    "Posting asyncOrchestratorAction to handle party traffic exchange for %s"
                        .formatted(conformanceRequest.path()));
                asyncOrchestratorAction(
                    newOrchestrator ->
                        newOrchestrator.handlePartyTrafficExchange(
                            new ConformanceExchange(conformanceRequest, conformanceResponse)));
              }
            })
        .exceptionally(
            e -> {
              log.error(
                  "ConformanceSandbox.asyncOutboundRequest(baseUrl='%s', path='%s') failed: %s"
                      .formatted(conformanceRequest.baseUrl(), conformanceRequest.path(), e),
                  e);
              return null;
            });
  }

  @SneakyThrows
  private ConformanceResponse syncHttpRequest(ConformanceRequest conformanceRequest) {
    HttpRequest.Builder httpRequestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(conformanceRequest.baseUrl() + conformanceRequest.path()))
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

  private ConformanceWebResponse _handlePartyRequest(
      ConformanceParty party, ConformanceWebRequest webRequest) {
    log.info("ConformanceSandbox._handlePartyRequest(%s)".formatted(party.getName()));
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
    ConformanceResponse conformanceResponse = party.handleRequest(conformanceRequest);
    if (conformanceOrchestrator != null) {
      conformanceOrchestrator.handlePartyTrafficExchange(
          new ConformanceExchange(conformanceRequest, conformanceResponse));
    }
    return new ConformanceWebResponse(
        conformanceResponse.statusCode(),
        JSON_UTF_8,
        conformanceResponse.message().headers(),
        conformanceResponse.message().body().getStringBody());
  }

  private ConformanceWebResponse _handleRequest(ConformanceWebRequest conformanceWebRequest) {
    Map<String, Function<ConformanceWebRequest, ConformanceWebResponse>> webHandlersByPathPrefix =
        new HashMap<>();
    parties.forEach(
        party -> {
          webHandlersByPathPrefix.put(
              "/conformance/sandbox/%s/party/%s/notification".formatted(id, party.getName()),
              webRequest ->
                  new ConformanceWebResponse(
                      200,
                      JSON_UTF_8,
                      Collections.emptyMap(),
                      party.handleNotification().toString()));

          webHandlersByPathPrefix.put(
              "/conformance/sandbox/%s/party/%s/from/%s"
                  .formatted(id, party.getName(), party.getCounterpartName()),
              webRequest -> this._handlePartyRequest(party, webRequest));
        });

    webHandlersByPathPrefix.put(
        "/conformance/sandbox/%s/orchestrator/reset".formatted(id),
        webRequest -> {
          _reset(true);
          return new ConformanceWebResponse(
              200,
              JSON_UTF_8,
              Collections.emptyMap(),
              new ObjectMapper().createObjectNode().toString());
        });

    if (conformanceOrchestrator != null) {
      webHandlersByPathPrefix.put(
          "/conformance/sandbox/%s/orchestrator/status".formatted(id),
          webRequest ->
              new ConformanceWebResponse(
                  200,
                  JSON_UTF_8,
                  Collections.emptyMap(),
                  conformanceOrchestrator.getStatus().toString()));

      Stream.concat(
              Arrays.stream(sandboxConfiguration.getParties()).map(PartyConfiguration::getName),
              Arrays.stream(sandboxConfiguration.getCounterparts())
                  .map(CounterpartConfiguration::getName))
          .collect(Collectors.toSet())
          .forEach(
              partyOrCounterpartName -> {
                webHandlersByPathPrefix.put(
                    "/conformance/sandbox/%s/orchestrator/party/%s/prompt/json"
                        .formatted(id, partyOrCounterpartName),
                    webRequest ->
                        new ConformanceWebResponse(
                            200,
                            JSON_UTF_8,
                            Collections.emptyMap(),
                            conformanceOrchestrator
                                .handleGetPartyPrompt(partyOrCounterpartName)
                                .toString()));
                webHandlersByPathPrefix.put(
                    "/conformance/sandbox/%s/orchestrator/party/%s/input"
                        .formatted(id, partyOrCounterpartName),
                    webRequest ->
                        new ConformanceWebResponse(
                            200,
                            JSON_UTF_8,
                            Collections.emptyMap(),
                            conformanceOrchestrator
                                .handlePartyInput(
                                    new ConformanceMessageBody(webRequest.body()).getJsonBody())
                                .toString()));
              });

      webHandlersByPathPrefix.put(
          "/conformance/sandbox/%s/orchestrator/report".formatted(id),
          webRequest ->
              new ConformanceWebResponse(
                  200,
                  "text/html;charset=utf-8",
                  Collections.emptyMap(),
                  conformanceOrchestrator.generateReport(
                      (sandboxConfiguration.getParties().length
                                  == EblSurrenderV10Role.values().length
                              ? Arrays.stream(EblSurrenderV10Role.values())
                                  .map(EblSurrenderV10Role::getConfigName)
                              : Arrays.stream(sandboxConfiguration.getCounterparts())
                                  .map(CounterpartConfiguration::getRole)
                                  .filter(
                                      counterpartRole ->
                                          Arrays.stream(sandboxConfiguration.getParties())
                                              .map(PartyConfiguration::getRole)
                                              .noneMatch(
                                                  partyRole ->
                                                      Objects.equals(partyRole, counterpartRole))))
                          .collect(Collectors.toSet()))));
    }
    return webHandlersByPathPrefix
        .get(
            webHandlersByPathPrefix.keySet().stream()
                .filter(conformanceWebRequest.uri()::startsWith)
                .findFirst()
                .orElseThrow(
                    () ->
                        new NullPointerException(
                            "Web handler not found for URI '%s'"
                                .formatted(conformanceWebRequest.uri()))))
        .apply(conformanceWebRequest);
  }
}
