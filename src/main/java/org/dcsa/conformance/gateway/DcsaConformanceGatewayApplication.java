package org.dcsa.conformance.gateway;

import java.util.Map;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import org.dcsa.conformance.gateway.analysis.ConformanceTrafficAnalyzer;
import org.dcsa.conformance.gateway.configuration.GatewayConfiguration;
import org.dcsa.conformance.gateway.analysis.ConformanceReport;
import org.dcsa.conformance.gateway.traffic.ConformanceTrafficRecorder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@SpringBootApplication
@ConfigurationPropertiesScan("org.dcsa.conformance.gateway.configuration")
public class DcsaConformanceGatewayApplication {

  ConformanceTrafficRecorder trafficRecorder = new ConformanceTrafficRecorder();
  GatewayConfiguration gatewayConfiguration;

  @Bean
  public RouteLocator myRoutes(
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

  @SneakyThrows
  @GetMapping("/report/json")
  public String reportJson(
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
  public String reportHtml(
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

  public static void main(String[] args) {
    SpringApplication.run(DcsaConformanceGatewayApplication.class, args);
  }
}
/*
checkDefinition
    protocol
    summary
    description
    messageMatchPredicate
    messageCheckPredicate
    subCheckDefinitions
checkResult
    checkDefinition
    passedMessages
    failedMessages
    subCheckResults
analysis
    protocol
    links
    startDateTime
    endDateTime
    checkDefinition
    checkResult
    ignoredMessages
 */
