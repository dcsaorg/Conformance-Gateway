package org.dcsa.conformance.gateway.standards.eblsurrender.v10;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.dcsa.conformance.gateway.check.ConformanceCheck;
import org.dcsa.conformance.gateway.check.ConformanceResult;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;
import org.springframework.util.MultiValueMap;

public class EblSurrenderV10ConformanceCheck extends ConformanceCheck {
  public EblSurrenderV10ConformanceCheck() {
    super("EBL Surrender V1.0");
  }

  @Override
  protected Stream<ConformanceCheck> createSubChecks() {
    return Stream.of(
        new ConformanceCheck("Async platform requests (platform-initiated sync exchanges)") {
          @Override
          protected Stream<ConformanceCheck> createSubChecks() {
            return Stream.of(
                createApiVersionHeaderCheck(),
                new ConformanceCheck("Async platform requests URL path is correct") {
                  @Override
                  protected void doCheck(ConformanceExchange exchange) {
                    if (EblSurrenderRole.isPlatform(exchange.getSourcePartyRole())) {
                      this.addResult(
                          ConformanceResult.forSourceParty(
                              exchange,
                              exchange.getRequestPath().endsWith("/v1/surrender-requests")));
                    }
                  }

                  @Override
                  public boolean isRelevantForRole(String roleName) {
                    return EblSurrenderRole.isPlatform(roleName);
                  }
                });
          }
        },
        new ConformanceCheck("Async carrier responses (carrier-initiated sync exchanges)") {
          @Override
          protected Stream<ConformanceCheck> createSubChecks() {
            return Stream.of(
                createApiVersionHeaderCheck(),
                new ConformanceCheck("Async carrier response URL path is correct") {
                  @Override
                  protected void doCheck(ConformanceExchange exchange) {
                    if (EblSurrenderRole.isCarrier(exchange.getSourcePartyRole())) {
                        this.addResult(
                          ConformanceResult.forSourceParty(
                              exchange,
                              exchange
                                  .getRequestPath()
                                  .endsWith("/v1/surrender-request-responses")));
                    }
                  }

                  @Override
                  public boolean isRelevantForRole(String roleName) {
                    return EblSurrenderRole.isCarrier(roleName);
                  }
                });
          }
        },
        new ConformanceCheck("Async exchanges (platform request, carrier response)") {
          @Override
          protected Stream<ConformanceCheck> createSubChecks() {
            return Stream.of(
                new ConformanceCheck(
                    "The surrenderRequestReference of every async response must match that of an async request") {
                  private Set<String> knownSurrenderRequestReferences = new TreeSet<>();

                  @SneakyThrows
                  @Override
                  protected void doCheck(ConformanceExchange exchange) {
                    String surrenderRequestReference =
                        new ObjectMapper()
                            .readTree(exchange.getRequestBody())
                            .get("surrenderRequestReference")
                            .asText();
                    if (EblSurrenderRole.isPlatform(exchange.getSourcePartyRole())) {
                      knownSurrenderRequestReferences.add(surrenderRequestReference);
                    } else if (EblSurrenderRole.isCarrier(exchange.getSourcePartyRole())) {
                        this.addResult(
                          ConformanceResult.forSourceParty(
                              exchange,
                              knownSurrenderRequestReferences.contains(surrenderRequestReference)));
                    }
                  }

                  @Override
                  public boolean isRelevantForRole(String roleName) {
                    return EblSurrenderRole.isCarrier(roleName);
                  }
                });
          }
        },
        new ConformanceCheck(
            "Async exchange workflows (amendment exchanges + surrender exchanges)"));
  }

  private ConformanceCheck createApiVersionHeaderCheck() {
    return new ConformanceCheck(
        "All sync requests and responses must contain Api-Version headers with a compatible version") {
      @Override
      protected Stream<ConformanceCheck> createSubChecks() {
        return Stream.of(
            new ConformanceCheck(
                "All sync requests must contain an Api-Version header with a compatible version") {
              @Override
              protected void doCheck(ConformanceExchange exchange) {
                  this.addResult(
                    ConformanceResult.forSourceParty(
                        exchange, checkApiVersionHeader(exchange.getRequestHeaders())));
              }
            },
            new ConformanceCheck(
                "All sync responses must contain an Api-Version header with a compatible version") {
              @Override
              protected void doCheck(ConformanceExchange exchange) {
                  this.addResult(
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
