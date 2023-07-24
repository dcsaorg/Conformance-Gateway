package org.dcsa.conformance.gateway;

import java.util.stream.Stream;

import org.dcsa.conformance.gateway.configuration.GatewayConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
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

  @GetMapping("/analyze")
  public String analyze(
          @RequestParam("standard") String standardName,
          @RequestParam("version") String standardVersion,
          @RequestParam("party") String partyName,
          @RequestParam("role") String roleName
  ) {
    return new ConformanceTrafficAnalyzer(gatewayConfiguration, standardName, standardVersion, partyName)
        .analyze(partyName, roleName, trafficRecorder.getTrafficStream())
        .toString();
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
