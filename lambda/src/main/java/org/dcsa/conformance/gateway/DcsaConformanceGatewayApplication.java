package org.dcsa.conformance.gateway;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
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
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.gateway.configuration.ConformanceConfiguration;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@SpringBootApplication
@ConfigurationPropertiesScan("org.dcsa.conformance.gateway.configuration")
public class DcsaConformanceGatewayApplication {
  @Autowired ConformanceConfiguration conformanceConfiguration;
  private ConformanceOrchestrator conformanceOrchestrator;
  private final Map<String, BiConsumer<HttpServletRequest, HttpServletResponse>>
      webHandlersByPathPrefix = new HashMap<>();

  @PostConstruct
  public void postConstruct() {
    log.info(
        "DcsaConformanceGatewayApplication.createRouteLocator(%s)"
            .formatted(Objects.requireNonNull(conformanceConfiguration)));

    List<ConformanceParty> conformanceParties =
        ConformancePartyFactory.createParties(
            conformanceConfiguration.getStandard(),
            conformanceConfiguration.getParties(),
            conformanceConfiguration.getCounterparts(),
            this::asyncOutboundRequest);

    conformanceParties.forEach(
        party -> {
          webHandlersByPathPrefix.put(
              "/conformance/party/%s/notification".formatted(party.getName()),
              ((servletRequest, servletResponse) -> party.handleNotification()));
          webHandlersByPathPrefix.put(
              "/conformance/party/%s/from/%s"
                  .formatted(party.getName(), party.getCounterpartName()),
              (servletRequest, servletResponse) ->
                  this._handlePartyRequest(party, servletRequest, servletResponse));
        });

    conformanceOrchestrator = new ConformanceOrchestrator(conformanceConfiguration);

    webHandlersByPathPrefix.put(
        "/conformance/orchestrator/reset",
        (servletRequest, servletResponse) -> conformanceOrchestrator.reset());

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
                  (servletRequest, servletResponse) ->
                      _writeResponse(
                          servletResponse,
                          HttpServletResponse.SC_OK,
                          "application/json;charset=utf-8",
                          Collections.emptyMap(),
                          conformanceOrchestrator
                              .handleGetPartyPrompt(partyOrCounterpartName)
                              .toPrettyString()));
              webHandlersByPathPrefix.put(
                  "/conformance/orchestrator/party/%s/input".formatted(partyOrCounterpartName),
                  (servletRequest, servletResponse) ->
                      conformanceOrchestrator.handlePartyInput(
                          new ConformanceMessageBody(_getRequestBody(servletRequest))
                              .getJsonBody()));
            });

    webHandlersByPathPrefix.put(
        "/conformance/orchestrator/report",
        (servletRequest, servletResponse) ->
            _writeResponse(
                servletResponse,
                HttpServletResponse.SC_OK,
                "text/html;charset=utf-8",
                Collections.emptyMap(),
                conformanceOrchestrator.generateReport(
                    (conformanceConfiguration.getParties().length
                                == EblSurrenderV10Role.values().length
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
                        .collect(Collectors.toSet()))));
  }

  private void _handlePartyRequest(
      ConformanceParty party,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) {
    ConformanceRequest conformanceRequest =
        new ConformanceRequest(
            servletRequest.getMethod(),
            servletRequest.getRequestURL().toString(),
            servletRequest.getRequestURI(),
            _getQueryParameters(servletRequest),
            new ConformanceMessage(
                party.getCounterpartName(),
                party.getCounterpartRole(),
                party.getName(),
                party.getRole(),
                _getRequestHeaders(servletRequest),
                new ConformanceMessageBody(_getRequestBody(servletRequest)),
                System.currentTimeMillis()));
    ConformanceResponse conformanceResponse = party.handleRequest(conformanceRequest);
    _writeResponse(servletResponse, conformanceResponse);
    conformanceOrchestrator.handlePartyTrafficExchange(
        new ConformanceExchange(conformanceRequest, conformanceResponse));
  }

  @SneakyThrows
  private static void _writeResponse(
      HttpServletResponse servletResponse,
      int statusCode,
      String contentType,
      Map<String, ? extends Collection<String>> headers,
      String stringBody) {
    servletResponse.setStatus(statusCode);
    servletResponse.setContentType(contentType);
    headers.forEach(
        (headerName, headerValues) ->
            headerValues.forEach(
                headerValue -> servletResponse.setHeader(headerName, headerValue)));
    PrintWriter writer = servletResponse.getWriter();
    writer.write(stringBody);
    writer.flush();
  }

  private static void _writeResponse(
      HttpServletResponse servletResponse, ConformanceResponse conformanceResponse) {
    _writeResponse(
        servletResponse,
        conformanceResponse.statusCode(),
        "application/json;charset=utf-8",
        conformanceResponse.message().headers(),
        conformanceResponse.message().body().getStringBody());
  }

  @RequestMapping(value = "/conformance/**")
  public void handleRequest(
      HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
    String uri = servletRequest.getRequestURI();
    webHandlersByPathPrefix
        .get(
            webHandlersByPathPrefix.keySet().stream()
                .filter(uri::startsWith)
                .findFirst()
                .orElseThrow(
                    () ->
                        new NullPointerException(
                            "Web handler not found for URI '%s'".formatted(uri))))
        .accept(servletRequest, servletResponse);
  }

  private static Map<String, List<String>> _getQueryParameters(HttpServletRequest request) {
    return request.getParameterMap().entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), Arrays.asList(entry.getValue())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @SneakyThrows
  private static String _getRequestBody(HttpServletRequest request) {
    return request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
  }

  private static Map<String, List<String>> _getRequestHeaders(HttpServletRequest request) {
    return Collections.list(request.getHeaderNames()).stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                headerName -> Collections.list(request.getHeaders(headerName))));
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

  public static void main(String[] args) {
    SpringApplication.run(DcsaConformanceGatewayApplication.class, args);
  }
}
