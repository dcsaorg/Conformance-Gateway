package org.dcsa.conformance.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "gateway.targetUrl=http://localhost:${wiremock.server.port}",
      "gateway.links[0].sourceParty.name=Platform1",
      "gateway.links[0].sourceParty.role=Platform",
      "gateway.links[0].targetParty.name=Carrier1",
      "gateway.links[0].targetParty.role=Carrier",
      "gateway.links[0].gatewayBasePath=/RequestLink/gateway",
      "gateway.links[0].targetBasePath=/RequestLink/target",
      "gateway.links[0].targetRootUrl=http://localhost:${wiremock.server.port}",
      "gateway.links[1].sourceParty.name=Carrier1",
      "gateway.links[1].sourceParty.role=Carrier",
      "gateway.links[1].targetParty.name=Platform1",
      "gateway.links[1].targetParty.role=Platform",
      "gateway.links[1].gatewayBasePath=/ResponseLink/gateway",
      "gateway.links[1].targetBasePath=/ResponseLink/target",
      "gateway.links[1].targetRootUrl=http://localhost:${wiremock.server.port}",
    })
@AutoConfigureWireMock(port = 0)
public class DcsaConformanceGatewayApplicationTests {

  @Autowired private WireMockServer wireMockServer;

  @Autowired private WebTestClient webTestClient;

  @Test
  void generatedTrafficHandled() {
    System.out.println("Using wireMockServer: " + wireMockServer);
    wireMockServer.addMockServiceRequestListener(
        (request, response) -> {
          System.out.println("================ WireMock request ================");
          System.out.println(request.getMethod());
          System.out.println(request.getUrl());
          System.out.println("--------------------------------------------------");
          System.out.println(request.getHeaders());
          System.out.println("--------------------------------------------------");
          System.out.println(request.getBodyAsString());
          System.out.println("--------------------------------------------------");
          System.out.println("================ WireMock response ================");
          System.out.println(response.getHeaders());
          System.out.println("---------------------------------------------------");
          System.out.println(response.getBodyAsString());
          System.out.println("---------------------------------------------------");
        });

    String standardName = "EblSurrender";
    String standardVersion = "1.0";

    TrafficGeneratorFactory.create(standardName, standardVersion)
        .get()
        .forEach(
            exchange -> {
              stubFor(
                  post(anyUrl())
                      .willReturn(
                          aResponse()
                              .withHeader("Content-Type", "application/json")
                              .withHeader("RequestUrl", "{{request.url}}")
                              .withHeader(
                                  "ResponseKey",
                                  "Response on link %s for request path %s"
                                      .formatted(exchange.getLink(), exchange.getRequestPath()))
                              .withBody(exchange.getResponseBody())
                              .withTransformers("response-template")));
              webTestClient
                  .post()
                  .uri("/%s/gateway%s".formatted(exchange.getLink(), exchange.getRequestPath()))
                  .header("RequestKey", "Request on link " + exchange.getLink())
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON)
                  .body(Mono.just(exchange.getRequestBody()), String.class)
                  .exchange()
                  .expectStatus()
                  .isOk()
                  .expectBody()
                  .json(exchange.getResponseBody());
            });
    webTestClient
        .get()
        .uri(
            "/report/json?standard=%s&version=%s&roles=Carrier&roles=Platform"
                .formatted(standardName, standardVersion))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody();
    webTestClient
        .get()
        .uri(
            "/report/html?standard=%s&version=%s&roles=Carrier&roles=Platform"
                .formatted(standardName, standardVersion))
        .accept(MediaType.TEXT_HTML)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody();
  }
}
