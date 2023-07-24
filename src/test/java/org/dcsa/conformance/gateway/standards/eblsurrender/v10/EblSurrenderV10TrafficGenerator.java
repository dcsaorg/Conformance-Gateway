package org.dcsa.conformance.gateway.standards.eblsurrender.v10;

import java.util.stream.Stream;

import lombok.Builder;
import org.dcsa.conformance.gateway.GeneratedTrafficExchange;
import org.dcsa.conformance.gateway.TrafficGenerator;

public class EblSurrenderV10TrafficGenerator implements TrafficGenerator {
  @Override
  public Stream<GeneratedTrafficExchange> get() {
    return Stream.of(
            normalRequestResponse(1),
            normalRequestResponse(2),
            normalRequestResponse(3)
    ).flatMap(s -> s);
  }

  private Stream<GeneratedTrafficExchange> normalRequestResponse(int index) {
    String scenarioName = "normalRequestResponse" + index;
    return Stream.of(
        GeneratedTrafficExchange.builder().link("RequestLink")
            .requestPath("/v1/surrender-requests")
            .requestBody(SurrenderRequestRequest.builder().scenario(scenarioName).build().toString())
            .responseBody(SurrenderRequestResponse.builder().scenario(scenarioName).build().toString())
            .build(),
        GeneratedTrafficExchange.builder().link("ResponseLink")
                .requestPath("/v1/surrender-request-responses")
                .requestBody(SurrenderResponseRequest.builder().scenario(scenarioName).build().toString())
                .responseBody(SurrenderResponseResponse.builder().build().toString())
                .build()
    );
  }

  @Builder
  private static class SurrenderRequestRequest {
    private String scenario;
    private boolean amended = false;
    @Override public String toString() {
      return "{\"surrenderRequestReference\": \"%s\", \"transportDocumentReference\": \"%s\", \"surrenderRequestCode\": \"%s\"}"
              .formatted("SRR-" + scenario, "TDR-" + scenario, amended ? "AREQ" : "SREQ");
    }
  }

  @Builder
  private static class SurrenderRequestResponse {
    private String scenario;
    @Override public String toString() {
      return "{\"surrenderRequestReference\": \"%s\", \"transportDocumentReference\": \"%s\"}"
              .formatted("SRR-" + scenario, "TDR-" + scenario);
    }
  }

  @Builder
  private static class SurrenderResponseRequest {
    private String scenario;
    private boolean rejected = false;
    @Override public String toString() {
      return "{\"surrenderRequestReference\": \"%s\", \"action\": \"%s\"}"
              .formatted("SRR-" + scenario, "ACT-" + scenario, rejected ? "SURR" : "SREJ");
    }
  }

  @Builder
  private static class SurrenderResponseResponse {
    @Override public String toString() {
      return "";
    }
  }
}
