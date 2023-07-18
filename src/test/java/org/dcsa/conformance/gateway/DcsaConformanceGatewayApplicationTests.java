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
            "gateway.links[0].sourceParty.name=Carrier C",
            "gateway.links[0].targetParty.name=Carrier K",
            "gateway.links[0].gatewayBasePath=/link0/gateway",
            "gateway.links[0].targetBasePath=/link0/target",
            "gateway.links[0].targetRootUrl=http://localhost:${wiremock.server.port}",
            "gateway.links[1].sourceParty.name=Carrier K",
            "gateway.links[1].targetParty.name=Carrier C",
            "gateway.links[1].gatewayBasePath=/link1/gateway",
            "gateway.links[1].targetBasePath=/link1/target",
            "gateway.links[1].targetRootUrl=http://localhost:${wiremock.server.port}",
            "gateway.links[2].sourceParty.name=Feeder F",
            "gateway.links[2].targetParty.name=Carrier C",
            "gateway.links[2].gatewayBasePath=/link2/gateway",
            "gateway.links[2].targetBasePath=/link2/target",
            "gateway.links[2].targetRootUrl=http://localhost:${wiremock.server.port}",
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

    String standardName = "Example";
    String standardVersion = "1.2";

    Stream.of(0, 1, 2)
        .limit(111)
        .forEach(
            linkIndex -> {
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
                                            "Response on link %d for request path %s"
                                                .formatted(linkIndex, exchange.getRequestPath()))
                                        .withBody(exchange.getResponseBody())
                                        .withTransformers("response-template")));
                        webTestClient
                            .post()
                            .uri("/link" + linkIndex + "/gateway" + exchange.getRequestPath())
                            .header("RequestKey", "Request on link " + linkIndex)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(exchange.getRequestBody()), String.class)
                            .exchange()
                            .expectStatus()
                            .isOk()
                            .expectBody()
                            .json(exchange.getResponseBody());
                      });
            });
    webTestClient
        .get()
        .uri(
            "/analyze?standard=%s&version=%s&party=Carrier C"
                .formatted(standardName, standardVersion))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json("{\"exchangeCount\": 6}");
  }
}
