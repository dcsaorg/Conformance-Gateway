package org.dcsa.conformance.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.report.ConformanceReport;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.gateway.configuration.ConformanceConfiguration;
import org.dcsa.conformance.gateway.configuration.StandardConfiguration;
import org.dcsa.conformance.standards.eblsurrender.v10.EblSurrenderV10ConformanceCheck;
import org.dcsa.conformance.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10Carrier;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10Platform;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10ScenarioListBuilder;
import org.dcsa.conformance.standards.eblsurrender.v10.scenario.SupplyAvailableTdrAction;
import org.dcsa.conformance.standards.eblsurrender.v10.scenario.VoidAndReissueAction;

@Slf4j
public class ConformanceOrchestrator {
  private final boolean inactive;
  private final ConformanceConfiguration conformanceConfiguration;
  protected final ScenarioListBuilder<?> scenarioListBuilder;
  protected final List<ConformanceScenario> scenarios = new ArrayList<>();
  private final ConformanceTrafficRecorder trafficRecorder;
  private final Map<String, CounterpartConfiguration> counterpartConfigurationsByPartyName;

  private final Map<String, Function<ConformanceWebRequest, ConformanceWebResponse>>
      webHandlersByPathPrefix = new HashMap<>();

  public ConformanceOrchestrator(ConformanceConfiguration conformanceConfiguration) {
    this.inactive = conformanceConfiguration.getOrchestrator() == null;
    this.conformanceConfiguration = conformanceConfiguration;

    this.scenarioListBuilder =
        inactive
            ? null
            : createScenarioListBuilder(
                conformanceConfiguration.getStandard(),
                conformanceConfiguration.getParties(),
                conformanceConfiguration.getCounterparts());

    trafficRecorder = inactive ? null : new ConformanceTrafficRecorder();

    counterpartConfigurationsByPartyName =
        Arrays.stream(conformanceConfiguration.getCounterparts())
            .collect(Collectors.toMap(CounterpartConfiguration::getName, Function.identity()));

    _createParties(
            conformanceConfiguration.getStandard(),
            conformanceConfiguration.getParties(),
            conformanceConfiguration.getCounterparts(),
            this::asyncOutboundRequest)
        .forEach(
            party -> {
              webHandlersByPathPrefix.put(
                  "/conformance/party/%s/notification".formatted(party.getName()),
                  (webRequest -> _handlePartyNotification(party)));
              webHandlersByPathPrefix.put(
                  "/conformance/party/%s/from/%s"
                      .formatted(party.getName(), party.getCounterpartName()),
                  webRequest -> this._handlePartyRequest(party, webRequest));
            });

    webHandlersByPathPrefix.put("/conformance/orchestrator/reset", webRequest -> reset());

    Stream.concat(
            Arrays.stream(conformanceConfiguration.getParties()).map(PartyConfiguration::getName),
            Arrays.stream(conformanceConfiguration.getCounterparts())
                .map(CounterpartConfiguration::getName))
        .collect(Collectors.toSet())
        .forEach(
            partyOrCounterpartName -> {
              webHandlersByPathPrefix.put(
                  "/conformance/orchestrator/party/%s/prompt/json"
                      .formatted(partyOrCounterpartName),
                  webRequest -> _handleGetPartyPrompt(partyOrCounterpartName));
              webHandlersByPathPrefix.put(
                  "/conformance/orchestrator/party/%s/input".formatted(partyOrCounterpartName),
                  webRequest ->
                      _handlePartyInput(
                          new ConformanceMessageBody(webRequest.body()).getJsonBody()));
            });

    webHandlersByPathPrefix.put(
        "/conformance/orchestrator/report",
        webRequest ->
            _generateReport(
                (conformanceConfiguration.getParties().length == EblSurrenderV10Role.values().length
                        ? Arrays.stream(EblSurrenderV10Role.values())
                            .map(EblSurrenderV10Role::getConfigName)
                        : Arrays.stream(conformanceConfiguration.getCounterparts())
                            .map(CounterpartConfiguration::getRole)
                            .filter(
                                counterpartRole ->
                                    Arrays.stream(conformanceConfiguration.getParties())
                                        .map(PartyConfiguration::getRole)
                                        .noneMatch(
                                            partyRole ->
                                                Objects.equals(partyRole, counterpartRole))))
                    .collect(Collectors.toSet())));
  }

  private void asyncOutboundRequest(
      ConformanceRequest conformanceRequest,
      Consumer<ConformanceResponse> conformanceResponseConsumer) {
    CompletableFuture.runAsync(
            () -> {
              log.info(
                  "DcsaConformanceGatewayApplication.outboundAsyncRequest(%s)"
                      .formatted(conformanceRequest));

              ConformanceResponse conformanceResponse = syncHttpRequest(conformanceRequest);

              conformanceResponseConsumer.accept(conformanceResponse);

              if (!conformanceRequest.message().targetPartyRole().equals("orchestrator")
                  && Arrays.stream(conformanceConfiguration.getParties())
                      .noneMatch(
                          partyConfiguration ->
                              Objects.equals(
                                  partyConfiguration.getName(),
                                  conformanceRequest.message().targetPartyName()))) {
                ConformanceOrchestrator.this.handlePartyTrafficExchange(
                    new ConformanceExchange(conformanceRequest, conformanceResponse));
              }
            })
        .exceptionally(
            e -> {
              log.error(
                  "DcsaConformanceGatewayApplication.outboundAsyncRequest(baseUrl='%s', path='%s') failed: %s"
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

  private ConformanceWebResponse _handlePartyNotification(ConformanceParty party) {
    party.handleNotification();
    return _emptyResponse();
  }

  private ConformanceWebResponse _emptyResponse() {
    return new ConformanceWebResponse(
        200,
        "application/json;charset=utf-8",
        Collections.emptyMap(),
        new ObjectMapper().createObjectNode().toString());
  }

  private ConformanceWebResponse _handlePartyRequest(
      ConformanceParty party, ConformanceWebRequest webRequest) {
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
    handlePartyTrafficExchange(new ConformanceExchange(conformanceRequest, conformanceResponse));
    return new ConformanceWebResponse(
        conformanceResponse.statusCode(),
        "application/json;charset=utf-8",
        conformanceResponse.message().headers(),
        conformanceResponse.message().body().getStringBody());
  }

  public ConformanceWebResponse handleRequest(ConformanceWebRequest conformanceWebRequest) {
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

  private static List<ConformanceParty> _createParties(
      StandardConfiguration standardConfiguration,
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      BiConsumer<ConformanceRequest, Consumer<ConformanceResponse>> asyncWebClient) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {

      Map<String, PartyConfiguration> partyConfigurationsByRoleName =
          Arrays.stream(partyConfigurations)
              .collect(Collectors.toMap(PartyConfiguration::getRole, Function.identity()));
      Map<String, CounterpartConfiguration> counterpartConfigurationsByRoleName =
          Arrays.stream(counterpartConfigurations)
              .collect(Collectors.toMap(CounterpartConfiguration::getRole, Function.identity()));

      LinkedList<ConformanceParty> parties = new LinkedList<>();

      PartyConfiguration carrierConfiguration =
          partyConfigurationsByRoleName.get(EblSurrenderV10Role.CARRIER.getConfigName());
      if (carrierConfiguration != null) {
        parties.add(
            new EblSurrenderV10Carrier(
                carrierConfiguration,
                counterpartConfigurationsByRoleName.get(
                    EblSurrenderV10Role.PLATFORM.getConfigName()),
                asyncWebClient));
      }

      PartyConfiguration platformConfiguration =
          partyConfigurationsByRoleName.get(EblSurrenderV10Role.PLATFORM.getConfigName());
      if (platformConfiguration != null) {
        parties.add(
            new EblSurrenderV10Platform(
                platformConfiguration,
                counterpartConfigurationsByRoleName.get(
                    EblSurrenderV10Role.CARRIER.getConfigName()),
                asyncWebClient));
      }

      return parties;
    }
    throw new UnsupportedOperationException(
        "Unsupported standard: %s".formatted(standardConfiguration));
  }

  public ConformanceWebResponse reset() {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    trafficRecorder.reset();

    scenarios.clear();
    scenarios.addAll(scenarioListBuilder.buildScenarioList());

    notifyAllPartiesOfNextActions();

    return _emptyResponse();
  }

  private synchronized void notifyAllPartiesOfNextActions() {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    scenarios.stream()
        .map(ConformanceScenario::peekNextAction)
        .filter(Objects::nonNull)
        .map(ConformanceAction::getSourcePartyName)
        .collect(Collectors.toSet())
        .forEach(this::asyncNotifyParty);
  }

  private void asyncNotifyParty(String partyName) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    CompletableFuture.runAsync(
            () -> {
              log.info("ConformanceOrchestrator.asyncNotifyParty(%s)".formatted(partyName));
              syncNotifyParty(partyName);
            })
        .exceptionally(
            e -> {
              log.error("Failed to notify party '%s': %s".formatted(partyName, e), e);
              return null;
            });
  }

  @SneakyThrows
  private void syncNotifyParty(String partyName) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    CounterpartConfiguration counterpartConfiguration =
        counterpartConfigurationsByPartyName.get(partyName);
    HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        counterpartConfiguration.getBaseUrl()
                            + counterpartConfiguration.getRootPath()
                            + "/party/%s/notification".formatted(partyName)))
                .timeout(Duration.ofHours(1))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  private synchronized ConformanceWebResponse _handleGetPartyPrompt(String partyName) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    log.info("ConformanceOrchestrator.handleGetPartyPrompt(%s)".formatted(partyName));
    return new ConformanceWebResponse(
        200,
        "application/json;charset=utf-8",
        Collections.emptyMap(),
        new ObjectMapper()
            .createArrayNode()
            .addAll(
                scenarios.stream()
                    .map(ConformanceScenario::peekNextAction)
                    .filter(Objects::nonNull)
                    .filter(action -> Objects.equals(action.getSourcePartyName(), partyName))
                    .map(ConformanceAction::asJsonNode)
                    .collect(Collectors.toList()))
            .toString());
  }

  private synchronized ConformanceWebResponse _handlePartyInput(JsonNode partyInput) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    log.info("ConformanceOrchestrator.handlePartyInput(%s)".formatted(partyInput.toPrettyString()));
    String actionId = partyInput.get("actionId").asText();
    ConformanceAction action =
        scenarios.stream()
            .filter(
                scenario ->
                    scenario.hasNextAction()
                        && Objects.equals(actionId, scenario.peekNextAction().getId().toString()))
            .map(ConformanceScenario::popNextAction)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Input for already handled(?) actionId %s: %s"
                            .formatted(actionId, partyInput.toPrettyString())));
    if (action instanceof SupplyAvailableTdrAction supplyAvailableTdrAction) {
      supplyAvailableTdrAction.getTdrConsumer().accept(partyInput.get("tdr").asText());
    } else if (action instanceof VoidAndReissueAction voidAndReissueAction) {
      voidAndReissueAction.getTdrConsumer().accept(partyInput.get("tdr").asText());
    } else {
      throw new UnsupportedOperationException(partyInput.toString());
    }
    notifyAllPartiesOfNextActions();
    return _emptyResponse();
  }

  public synchronized void handlePartyTrafficExchange(ConformanceExchange exchange) {
    if (inactive) return;
    trafficRecorder.recordExchange(exchange);
    ConformanceAction action =
        scenarios.stream()
            .filter(
                scenario ->
                    scenario.hasNextAction()
                        && scenario.peekNextAction().updateFromExchangeIfItMatches(exchange))
            .map(ConformanceScenario::popNextAction)
            .findFirst()
            .orElse(null);
    if (action == null) {
      log.info(
          "Ignoring conformance exchange not matched by any pending actions: %s"
              .formatted(exchange));
      return;
    }
    notifyAllPartiesOfNextActions();
  }

  private ConformanceWebResponse _generateReport(Set<String> roleNames) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");

    ConformanceCheck conformanceCheck =
        _createConformanceCheck(conformanceConfiguration.getStandard(), scenarioListBuilder);
    trafficRecorder.getTrafficStream().forEach(conformanceCheck::check);
    Map<String, ConformanceReport> reportsByRoleName =
        ConformanceReport.createForRoles(conformanceCheck, roleNames);

    return new ConformanceWebResponse(
        200,
        "text/html;charset=utf-8",
        Collections.emptyMap(),
        ConformanceReport.toHtmlReport(reportsByRoleName));
  }

  private static ConformanceCheck _createConformanceCheck(
      StandardConfiguration standardConfiguration, ScenarioListBuilder<?> scenarioListBuilder) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {
      return new EblSurrenderV10ConformanceCheck(scenarioListBuilder);
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'"
            .formatted(standardConfiguration.getName(), standardConfiguration.getVersion()));
  }

  public static ScenarioListBuilder<?> createScenarioListBuilder(
      StandardConfiguration standardConfiguration,
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {
      return EblSurrenderV10ScenarioListBuilder.buildTree(
          _findPartyOrCounterpartName(
              partyConfigurations, counterpartConfigurations, EblSurrenderV10Role::isCarrier),
          _findPartyOrCounterpartName(
              partyConfigurations, counterpartConfigurations, EblSurrenderV10Role::isPlatform));
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'"
            .formatted(standardConfiguration.getName(), standardConfiguration.getVersion()));
  }

  private static String _findPartyOrCounterpartName(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      Predicate<String> rolePredicate) {
    return Stream.concat(
            Arrays.stream(partyConfigurations)
                .filter(partyConfiguration -> rolePredicate.test(partyConfiguration.getRole()))
                .map(PartyConfiguration::getName),
            Arrays.stream(counterpartConfigurations)
                .filter(
                    counterpartConfigurationConfiguration ->
                        rolePredicate.test(counterpartConfigurationConfiguration.getRole()))
                .map(CounterpartConfiguration::getName))
        .findFirst()
        .orElseThrow();
  }
}
