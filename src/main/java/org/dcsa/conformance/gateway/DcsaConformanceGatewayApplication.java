package org.dcsa.conformance.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
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
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@SpringBootApplication
@ConfigurationPropertiesScan("org.dcsa.conformance.gateway.configuration")
public class DcsaConformanceGatewayApplication {

  private final ConformanceTrafficRecorder trafficRecorder = new ConformanceTrafficRecorder();
  private final ConformanceOrchestrator conformanceOrchestrator =
      new EblSurrenderV10Orchestrator("Platform1", "Carrier1", this::notifyParty);
  private final Map<String, ConformanceParty> conformancePartiesByName =
      Stream.of(
              new EblSurrenderV10Carrier(
                  "Carrier1", true, "http://localhost:9000", "/RequestLink/gateway"),
              new EblSurrenderV10Platform(
                  "Platform1", true, "http://localhost:9000", "/ResponseLink/gateway"))
          .collect(Collectors.toMap(ConformanceParty::getName, Function.identity()));

  GatewayConfiguration gatewayConfiguration;

  @Bean
  public RouteLocator createRouteLocator(
      RouteLocatorBuilder routeLocatorBuilder, GatewayConfiguration gatewayConfiguration) {

    System.out.println("Using gateway configuration: " + gatewayConfiguration);
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
                                              return Mono.just(requestBody);
                                            })
                                        .modifyResponseBody(
                                            String.class,
                                            String.class,
                                            (exchange, responseBody) -> {
                                              trafficRecorder.recordResponse(
                                                  exchange, responseBody);
                                              return Mono.just(responseBody);
                                            }))
                            .uri(link.getTargetRootUrl())));
    return routeLocatorBuilderBuilder.build();
  }

  @PostMapping(value = "/traffic/party/{partyName}/**", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<JsonNode> handlePartyPostRequest(
      @PathVariable String partyName, @RequestBody JsonNode requestBody) {
    return conformancePartiesByName.get(partyName).handlePostRequest(requestBody);
  }

  private void notifyParty(String partyName, JsonNode jsonNode) {
    WebTestClient.bindToServer()
        .baseUrl("http://localhost:9000") // FIXME use config / deployment variable URL
        .build()
        .post()
        .uri("/party/%s/notify".formatted(partyName))
        .contentType(MediaType.APPLICATION_JSON)
        // don't (notify only) .body(Mono.just(jsonNode), String.class)
        .exchange();
  }

  @GetMapping(value = "/party/{partyName}/notify")
  @ResponseBody
  public JsonNode handlePartyNotification(@PathVariable String partyName) {
    conformancePartiesByName.get(partyName).handleNotification();
    return new ObjectMapper().createObjectNode();
  }

  @SneakyThrows
  @GetMapping(value = "/party/{partyName}/prompt/json", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public JsonNode handleGetPartyPrompt(@PathVariable String partyName) {
    return conformanceOrchestrator.getPartyPrompt(partyName);
  }

  @SneakyThrows
  @PostMapping(value = "/party/input", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public JsonNode handlePostPartyInput(@RequestBody JsonNode partyInput) {
    return conformanceOrchestrator.postPartyInput(partyInput);
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
    System.out.println("################################################################");
    System.out.println("reports by role name = " + response);
    System.out.println("################################################################");
    return response;
  }

  @SneakyThrows
  @GetMapping(value = "/report/html", produces = MediaType.TEXT_HTML_VALUE)
  public String generateReportHtml(
      @RequestParam("standard") String standardName,
      @RequestParam("version") String standardVersion,
      @RequestParam("roles") String[] roleNames) {
    Map<String, ConformanceReport> reportsByRoleName =
        new ConformanceTrafficAnalyzer(standardName, standardVersion)
            .analyze(trafficRecorder.getTrafficStream(), roleNames);
    String htmlResponse = ConformanceReport.toHtmlReport(reportsByRoleName);
    System.out.println("################################################################");
    System.out.println("reports by role name = \n\n\n" + htmlResponse + "\n\n");
    System.out.println("################################################################");
    return htmlResponse;
  }

  @SneakyThrows
  @GetMapping(value = "/reset", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public JsonNode handleReset() {
    trafficRecorder.reset();
    conformanceOrchestrator.reset();
    return new ObjectMapper().readTree("{}");
  }

  public static void main(String[] args) {
    SpringApplication.run(DcsaConformanceGatewayApplication.class, args);
  }
}
