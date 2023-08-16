package org.dcsa.conformance.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.gateway.analysis.ConformanceReport;
import org.dcsa.conformance.gateway.analysis.ConformanceTrafficAnalyzer;
import org.dcsa.conformance.gateway.configuration.GatewayConfiguration;
import org.dcsa.conformance.gateway.parties.ConformanceOrchestrator;
import org.dcsa.conformance.gateway.parties.ConformanceParty;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10Carrier;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10Orchestrator;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10Platform;
import org.dcsa.conformance.gateway.traffic.ConformanceTrafficRecorder;
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

  private final ConformanceTrafficRecorder trafficRecorder = new ConformanceTrafficRecorder();
  private final ConformanceOrchestrator conformanceOrchestrator =
      new EblSurrenderV10Orchestrator("Platform1", "Carrier1");
  private final Map<String, ConformanceParty> conformancePartiesByName =
      Stream.of(
              new EblSurrenderV10Carrier(
                  "Carrier1", true, "http://localhost:8080", "/ResponseLink/gateway"),
              new EblSurrenderV10Platform(
                  "Platform1", true, "http://localhost:8080", "/RequestLink/gateway"))
          .collect(Collectors.toMap(ConformanceParty::getName, Function.identity()));

  GatewayConfiguration gatewayConfiguration;

  @Bean
  public RouteLocator createRouteLocator(
      RouteLocatorBuilder routeLocatorBuilder, GatewayConfiguration gatewayConfiguration) {

    log.info("Using gateway configuration: " + gatewayConfiguration);
    this.gatewayConfiguration = gatewayConfiguration;

    RouteLocatorBuilder.Builder routeLocatorBuilderBuilder = routeLocatorBuilder.routes();
    Stream.of(gatewayConfiguration.getLinks())
        .forEach(
            link ->
                routeLocatorBuilderBuilder.route(
                    "Route from %s to %s"
                        .formatted(
                            link.getSourceParty().getName(), link.getTargetParty().getName()),
                    route ->
                        route
                            .path(link.getGatewayBasePath() + "/**")
                            .filters(
                                f ->
                                    f.rewritePath(
                                            link.getGatewayBasePath() + "/(?<XP>.*)",
                                            link.getTargetBasePath() + "/${XP}")
                                        .modifyRequestBody(
                                            String.class,
                                            String.class,
                                            (exchange, requestBody) -> {
                                              trafficRecorder.recordRequest(
                                                  link.getSourceParty().getName(),
                                                  link.getSourceParty().getRole(),
                                                  link.getTargetParty().getName(),
                                                  link.getTargetParty().getRole(),
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
                            .uri(link.getTargetRootUrl())));
    return routeLocatorBuilderBuilder.build();
  }

  @PostMapping(value = "/traffic/party/{partyName}/**", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<JsonNode> handlePartyPostRequest(
      @PathVariable String partyName, @RequestBody JsonNode requestBody) {
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
            .analyze(trafficRecorder.getTrafficStream(), roleNames);
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

  @GetMapping(value = "/report/html", produces = MediaType.TEXT_HTML_VALUE)
  public String generateReportHtml(
      @RequestParam("standard") String standardName,
      @RequestParam("version") String standardVersion,
      @RequestParam("roles") String[] roleNames) {
    Map<String, ConformanceReport> reportsByRoleName =
        new ConformanceTrafficAnalyzer(standardName, standardVersion)
            .analyze(trafficRecorder.getTrafficStream(), roleNames);
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
