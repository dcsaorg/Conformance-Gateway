package org.dcsa.conformance.gateway.standards.eblsurrender.v10;

import java.util.List;
import java.util.stream.Stream;
import org.dcsa.conformance.gateway.*;
import org.springframework.util.MultiValueMap;

public class EblSurrenderV10ConformanceCheck extends ConformanceCheck {
  public EblSurrenderV10ConformanceCheck() {
    super("EBL Surrender V1.0");
  }

  @Override
  protected Stream<ConformanceCheck> getSubChecks() {
    return Stream.of(
        new ConformanceCheck("Async platform requests (platform-initiated sync exchanges)") {
          @Override
          protected Stream<ConformanceCheck> getSubChecks() {
            return Stream.of(
                createApiVersionHeaderCheck(),
                new ConformanceCheck("Async platform requests URL path is correct") {
                  @Override
                  protected void doCheck(ConformanceExchange exchange) {
                    results.add(
                        ConformanceResult.forSourceParty(
                            exchange,
                            exchange.getRequestPath().endsWith("/v1/surrender-requests")));
                  }
                });
          }
        },
        new ConformanceCheck("Async carrier responses (carrier-initiated sync exchanges)") {
          @Override
          protected Stream<ConformanceCheck> getSubChecks() {
            return Stream.of(
                createApiVersionHeaderCheck(),
                new ConformanceCheck("Async carrier response URL path is correct") {
                  @Override
                  protected void doCheck(ConformanceExchange exchange) {
                    results.add(
                        ConformanceResult.forSourceParty(
                            exchange,
                            exchange.getRequestPath().endsWith("/v1/surrender-request-responses")));
                  }
                });
          }
        },
        new ConformanceCheck("Async exchanges (platform request, carrier response)"),
        new ConformanceCheck("Async exchange workflows (amendment exchanges + surrender exchanges)"));
  }

  private ConformanceCheck createApiVersionHeaderCheck() {
    return new ConformanceCheck("All sync requests and responses must contain Api-Version headers with a compatible version") {
      @Override
      protected Stream<ConformanceCheck> getSubChecks() {
        return Stream.of(
            new ConformanceCheck("All sync requests must contain an Api-Version header with a compatible version") {
              @Override
              protected void doCheck(ConformanceExchange exchange) {
                this.results.add(
                    ConformanceResult.forSourceParty(
                        exchange, checkApiVersionHeader(exchange.getRequestHeaders())));
              }
            },
            new ConformanceCheck("All sync responses must contain an Api-Version header with a compatible version") {
              @Override
              protected void doCheck(ConformanceExchange exchange) {
                this.results.add(
                    ConformanceResult.forTargetParty(
                        exchange, checkApiVersionHeader(exchange.getResponseHeaders())));
              }
            });
      }

      private boolean checkApiVersionHeader(MultiValueMap<String, String> headers) {
        List<String> headerValues = headers.get("Api-Version");
        if (headerValues == null) return false;
        if (headerValues.size() != 1) return false;
        String apiVersion = headerValues.get(0);
        if (!"1.0".equals(apiVersion)) return false;
        return true;
      }
    };
  }
}
