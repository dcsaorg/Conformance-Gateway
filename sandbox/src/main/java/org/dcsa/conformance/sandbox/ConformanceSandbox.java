package org.dcsa.conformance.sandbox;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.dcsa.conformance.sandbox.configuration.StandardConfiguration;
import org.dcsa.conformance.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10Carrier;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10Platform;

@Slf4j
public class ConformanceSandbox {
  public static final String JSON_UTF_8 = "application/json;charset=utf-8";
  private final SandboxConfiguration sandboxConfiguration;
  private final ConformanceOrchestrator conformanceOrchestrator;
  private final Map<String, Function<ConformanceWebRequest, ConformanceWebResponse>>
      webHandlersByPathPrefix = new HashMap<>();

  public ConformanceSandbox(SandboxConfiguration sandboxConfiguration) {
    this.sandboxConfiguration = sandboxConfiguration;
    this.conformanceOrchestrator = new ConformanceOrchestrator(sandboxConfiguration);

    _createParties(
            sandboxConfiguration.getStandard(),
            sandboxConfiguration.getParties(),
            sandboxConfiguration.getCounterparts(),
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

    webHandlersByPathPrefix.put(
        "/conformance/orchestrator/reset",
        webRequest ->
            new ConformanceWebResponse(
                200,
                JSON_UTF_8,
                Collections.emptyMap(),
                conformanceOrchestrator.reset().toString()));

    Stream.concat(
            Arrays.stream(sandboxConfiguration.getParties()).map(PartyConfiguration::getName),
            Arrays.stream(sandboxConfiguration.getCounterparts())
                .map(CounterpartConfiguration::getName))
        .collect(Collectors.toSet())
        .forEach(
            partyOrCounterpartName -> {
              webHandlersByPathPrefix.put(
                  "/conformance/orchestrator/party/%s/prompt/json"
                      .formatted(partyOrCounterpartName),
                  webRequest ->
                      new ConformanceWebResponse(
                          200,
                          JSON_UTF_8,
                          Collections.emptyMap(),
                          conformanceOrchestrator
                              .handleGetPartyPrompt(partyOrCounterpartName)
                              .toString()));
              webHandlersByPathPrefix.put(
                  "/conformance/orchestrator/party/%s/input".formatted(partyOrCounterpartName),
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
        "/conformance/orchestrator/report",
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
                  && Arrays.stream(sandboxConfiguration.getParties())
                      .noneMatch(
                          partyConfiguration ->
                              Objects.equals(
                                  partyConfiguration.getName(),
                                  conformanceRequest.message().targetPartyName()))) {
                conformanceOrchestrator.handlePartyTrafficExchange(
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
    return new ConformanceWebResponse(
        200, JSON_UTF_8, Collections.emptyMap(), party.handleNotification().toString());
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
    conformanceOrchestrator.handlePartyTrafficExchange(
        new ConformanceExchange(conformanceRequest, conformanceResponse));
    return new ConformanceWebResponse(
        conformanceResponse.statusCode(),
        JSON_UTF_8,
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
}
