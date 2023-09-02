package org.dcsa.conformance.gateway.standards.eblsurrender.v10;

import java.util.*;
import java.util.stream.Stream;
import org.dcsa.conformance.gateway.check.ConformanceCheck;
import org.dcsa.conformance.gateway.check.ConformanceResult;
import org.dcsa.conformance.gateway.check.JsonSchemaValidator;
import org.dcsa.conformance.gateway.scenarios.ScenarioListBuilder;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

public class EblSurrenderV10ConformanceCheck extends ConformanceCheck {

  public static final String SCHEMAS_FOLDER = "/standards/eblsurrender/v10/";
  public static final String SCHEMAS_FILE_ASYNC_REQUEST =
      SCHEMAS_FOLDER + "eblsurrender-v10-async-request.json";
  public static final String SCHEMAS_FILE_ASYNC_RESPONSE =
      SCHEMAS_FOLDER + "eblsurrender-v10-async-response.json";

  private final ScenarioListBuilder<?> scenarioListBuilder;

  public EblSurrenderV10ConformanceCheck(ScenarioListBuilder<?> scenarioListBuilder) {
    super("EBL Surrender V1.0");
    this.scenarioListBuilder = scenarioListBuilder;
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.concat(
        Stream.of(scenarioListBuilder.buildRootCheckTree()), _createNonScenarioSubChecks());
  }

  private Stream<? extends ConformanceCheck> _createNonScenarioSubChecks() {
    return Stream.of(
        new ConformanceCheck("Async platform requests (platform-initiated sync exchanges)") {
          @Override
          protected Stream<? extends ConformanceCheck> createSubChecks() {
            return Stream.of(
                createApiVersionHeaderCheck(),
                new ConformanceCheck("Async platform request URL path is correct") {
                  @Override
                  protected void doCheck(ConformanceExchange exchange) {
                    if (EblSurrenderV10Role.isPlatform(
                        exchange.getRequest().message().sourcePartyRole())) {
                      this.addResult(
                          ConformanceResult.forSourceParty(
                              exchange,
                              exchange.getRequest().path().endsWith("/v1/surrender-requests")));
                    }
                  }

                  @Override
                  public boolean isRelevantForRole(String roleName) {
                    return EblSurrenderV10Role.isPlatform(roleName);
                  }
                },
                new ConformanceCheck("Async request sync request body matches JSON schema") {

                  private final JsonSchemaValidator jsonSchemaValidator =
                      new JsonSchemaValidator(
                          SCHEMAS_FILE_ASYNC_REQUEST, "surrenderRequestDetails");

                  @Override
                  protected void doCheck(ConformanceExchange exchange) {
                    if (EblSurrenderV10Role.isPlatform(
                        exchange.getRequest().message().sourcePartyRole())) {
                      this.addResult(
                          ConformanceResult.forSourceParty(
                              exchange,
                              jsonSchemaValidator.validate(
                                  exchange.getRequest().message().body().getStringBody())));
                    }
                  }

                  @Override
                  public boolean isRelevantForRole(String roleName) {
                    return EblSurrenderV10Role.isPlatform(roleName);
                  }
                },
                new ConformanceCheck("Async request sync response body matches JSON schema") {

                  private final JsonSchemaValidator jsonSchemaValidator =
                      new JsonSchemaValidator(
                          SCHEMAS_FILE_ASYNC_REQUEST, "surrenderRequestAcknowledgement");

                  @Override
                  protected void doCheck(ConformanceExchange exchange) {
                    if (EblSurrenderV10Role.isPlatform(
                        exchange.getRequest().message().sourcePartyRole())) {
                      this.addResult(
                          ConformanceResult.forTargetParty(
                              exchange,
                              jsonSchemaValidator.validate(
                                  exchange.getResponse().message().body().getStringBody())));
                    }
                  }

                  @Override
                  public boolean isRelevantForRole(String roleName) {
                    return EblSurrenderV10Role.isCarrier(roleName);
                  }
                },
                new ConformanceCheck("Async response sync request body matches JSON schema") {

                  private final JsonSchemaValidator jsonSchemaValidator =
                      new JsonSchemaValidator(
                          SCHEMAS_FILE_ASYNC_RESPONSE, "surrenderRequestAnswer");

                  @Override
                  protected void doCheck(ConformanceExchange exchange) {
                    if (EblSurrenderV10Role.isCarrier(
                        exchange.getRequest().message().sourcePartyRole())) {
                      this.addResult(
                          ConformanceResult.forSourceParty(
                              exchange,
                              jsonSchemaValidator.validate(
                                  exchange.getRequest().message().body().getStringBody())));
                    }
                  }

                  @Override
                  public boolean isRelevantForRole(String roleName) {
                    return EblSurrenderV10Role.isCarrier(roleName);
                  }
                });
          }
        },
        new ConformanceCheck("Async carrier responses (carrier-initiated sync exchanges)") {
          @Override
          protected Stream<? extends ConformanceCheck> createSubChecks() {
            return Stream.of(
                createApiVersionHeaderCheck(),
                new ConformanceCheck("Async carrier response URL path is correct") {
                  @Override
                  protected void doCheck(ConformanceExchange exchange) {
                    if (EblSurrenderV10Role.isCarrier(
                        exchange.getRequest().message().sourcePartyRole())) {
                      this.addResult(
                          ConformanceResult.forSourceParty(
                              exchange,
                              exchange
                                  .getRequest()
                                  .path()
                                  .endsWith("/v1/surrender-request-responses")));
                    }
                  }

                  @Override
                  public boolean isRelevantForRole(String roleName) {
                    return EblSurrenderV10Role.isCarrier(roleName);
                  }
                });
          }
        });
  }

  private ConformanceCheck createApiVersionHeaderCheck() {
    return new ConformanceCheck(
        "All sync requests and responses must contain Api-Version headers with a compatible version") {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new ConformanceCheck(
                "All sync requests must contain an Api-Version header with a compatible version") {
              @Override
              protected void doCheck(ConformanceExchange exchange) {
                this.addResult(
                    ConformanceResult.forSourceParty(
                        exchange,
                        checkApiVersionHeader(exchange.getRequest().message().headers())));
              }
            },
            new ConformanceCheck(
                "All sync responses must contain an Api-Version header with a compatible version") {
              @Override
              protected void doCheck(ConformanceExchange exchange) {
                this.addResult(
                    ConformanceResult.forTargetParty(
                        exchange,
                        checkApiVersionHeader(exchange.getResponse().message().headers())));
              }
            });
      }

      private boolean checkApiVersionHeader(Map<String, ? extends Collection<String>> headers) {
        String headerName =
            headers.keySet().stream()
                .filter(key -> key.equalsIgnoreCase("api-version"))
                .findFirst()
                .orElse("api-version");
        Collection<String> headerValues = headers.get(headerName);
        if (headerValues == null) return false;
        if (headerValues.size() != 1) return false;
        String apiVersion = headerValues.stream().findFirst().orElseThrow();
        return "1.0.0".equals(apiVersion);
      }
    };
  }
}
