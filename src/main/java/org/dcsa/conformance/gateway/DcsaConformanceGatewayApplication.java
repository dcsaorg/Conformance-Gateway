package org.dcsa.conformance.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.gateway.analysis.ConformanceReport;
import org.dcsa.conformance.gateway.analysis.ConformanceTrafficAnalyzer;
import org.dcsa.conformance.gateway.configuration.ConformanceConfiguration;
import org.dcsa.conformance.gateway.parties.ConformanceOrchestrator;
import org.dcsa.conformance.gateway.parties.ConformanceParty;
import org.dcsa.conformance.gateway.parties.ConformancePartyFactory;
import org.dcsa.conformance.gateway.traffic.ConformanceTrafficRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@SpringBootApplication
@ConfigurationPropertiesScan("org.dcsa.conformance.gateway.configuration")
public class DcsaConformanceGatewayApplication {
  @Autowired ConformanceConfiguration conformanceConfiguration;
  private ConformanceTrafficRecorder trafficRecorder;
  private ConformanceOrchestrator conformanceOrchestrator;
  private Map<String, ConformanceParty> conformancePartiesByName;

  @Bean
  public RouteLocator createRouteLocator(RouteLocatorBuilder routeLocatorBuilder) {
    log.info("DcsaConformanceGatewayApplication.createRouteLocator()");
    log.info("conformanceConfiguration = " + Objects.requireNonNull(conformanceConfiguration));
    trafficRecorder = new ConformanceTrafficRecorder();

    conformanceOrchestrator =
        new ConformanceOrchestrator(
            conformanceConfiguration.getStandard(), conformanceConfiguration.getOrchestrator());

    conformancePartiesByName =
        Arrays.stream(conformanceConfiguration.getParties())
            .map(
                partyConfiguration ->
                    ConformancePartyFactory.create(
                        conformanceConfiguration.getStandard(), partyConfiguration))
            .collect(Collectors.toMap(ConformanceParty::getName, Function.identity()));

    RouteLocatorBuilder.Builder routeLocatorBuilderBuilder = routeLocatorBuilder.routes();
    Stream.of(conformanceConfiguration.getGateway().getRoutes())
        .forEach(
            routeConfiguration ->
                routeLocatorBuilderBuilder.route(
                    "Route from %s to %s"
                        .formatted(
                            routeConfiguration.getSourcePartyName(),
                            routeConfiguration.getTargetPartyName()),
                    route ->
                        route
                            .path(routeConfiguration.getGatewayRootPath() + "/**")
                            .filters(
                                f ->
                                    f.rewritePath(
                                            routeConfiguration.getGatewayRootPath() + "/(?<XP>.*)",
                                            routeConfiguration.getTargetRootPath() + "/${XP}")
                                        .modifyRequestBody(
                                            String.class,
                                            String.class,
                                            (exchange, requestBody) -> {
                                              trafficRecorder.recordRequest(
                                                  routeConfiguration.getSourcePartyName(),
                                                  routeConfiguration.getSourcePartyRole(),
                                                  routeConfiguration.getTargetPartyName(),
                                                  routeConfiguration.getTargetPartyRole(),
                                                  exchange,
                                                  requestBody);
                                              return Mono.just(
                                                  Optional.ofNullable(requestBody).orElse(""));
                                            })
                                        .modifyResponseBody(
                                            String.class,
                                            String.class,
                                            (exchange, responseBody) -> {
                                              conformanceOrchestrator.handlePartyTrafficExchange(
                                                  trafficRecorder.recordResponse(
                                                      exchange, responseBody));
                                              return Mono.just(
                                                  Optional.ofNullable(responseBody).orElse(""));
                                            }))
                            .uri(routeConfiguration.getTargetBaseUrl())));
    return routeLocatorBuilderBuilder.build();
  }

  @PostMapping(value = "/traffic/party/{partyName}/**", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<JsonNode> handlePartyPostRequest(
      @PathVariable String partyName, @RequestBody JsonNode requestBody) {
    log.info(
        "DcsaConformanceGatewayApplication.handlePartyPostRequest(%s, %s)"
            .formatted(partyName, requestBody.toPrettyString()));
    return conformancePartiesByName.get(partyName).handleRegularTraffic(requestBody);
  }

  @GetMapping(value = "/party/{partyName}/notify")
  @ResponseBody
  public JsonNode handlePartyNotification(@PathVariable String partyName) {
    conformancePartiesByName.get(partyName).handleNotification();
    return new ObjectMapper().createObjectNode();
  }

  @GetMapping(value = "/party/{partyName}/prompt/json", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public JsonNode handleGetPartyPrompt(@PathVariable String partyName) {
    return conformanceOrchestrator.handleGetPartyPrompt(partyName);
  }

  @PostMapping(value = "/party/input", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public JsonNode handlePostPartyInput(@RequestBody JsonNode partyInput) {
    return conformanceOrchestrator.handlePartyInput(partyInput);
  }

  @SneakyThrows
  @GetMapping("/report/json")
  public String generateReportJson(
      @RequestParam("standard") String standardName,
      @RequestParam("version") String standardVersion,
      @RequestParam("roles") String[] roleNames) {
    Map<String, ConformanceReport> reportsByRoleName =
        new ConformanceTrafficAnalyzer(standardName, standardVersion)
            .analyze(
                conformanceOrchestrator.getScenarioListBuilder(),
                trafficRecorder.getTrafficStream(),
                roleNames);
    String response =
        Jackson2ObjectMapperBuilder.json()
            .indentOutput(true)
            .build()
            .writeValueAsString(reportsByRoleName);
    log.info("################################################################");
    log.info("reports by role name = " + response);
    log.info("################################################################");
    return response;
  }

  // test:
  // http://localhost:8080/report/html?standard=EblSurrender&version=1.0&roles=Carrier&roles=Platform
  @GetMapping(value = "/report/html", produces = MediaType.TEXT_HTML_VALUE)
  public String generateReportHtml(
      @RequestParam("standard") String standardName,
      @RequestParam("version") String standardVersion,
      @RequestParam("roles") String[] roleNames) {
    Map<String, ConformanceReport> reportsByRoleName =
        new ConformanceTrafficAnalyzer(standardName, standardVersion)
            .analyze(
                conformanceOrchestrator.getScenarioListBuilder(),
                trafficRecorder.getTrafficStream(),
                roleNames);
    String htmlResponse = ConformanceReport.toHtmlReport(reportsByRoleName);
    log.info("################################################################");
    log.info("reports by role name = \n\n\n" + htmlResponse + "\n\n");
    log.info("################################################################");
    return htmlResponse;
  }

  @GetMapping(value = "/reset", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public JsonNode handleReset() {
    trafficRecorder.reset();
    conformanceOrchestrator.reset();
    return new ObjectMapper().createObjectNode();
  }

  public static void main(String[] args) {
    SpringApplication.run(DcsaConformanceGatewayApplication.class, args);
  }
}
