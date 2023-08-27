package org.dcsa.conformance.gateway.standards.eblsurrender.v10;

import java.util.*;
import java.util.stream.Stream;
import org.dcsa.conformance.gateway.check.ConformanceCheck;
import org.dcsa.conformance.gateway.check.ConformanceResult;
import org.dcsa.conformance.gateway.check.JsonSchemaValidator;
import org.dcsa.conformance.gateway.scenarios.ScenarioListBuilder;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10ScenarioListBuilder;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;
import org.springframework.util.MultiValueMap;

public class EblSurrenderV10ConformanceCheck extends ConformanceCheck {

  public static final String SCHEMAS_FOLDER = "/schemas/eblsurrender/v10/";
  public static final String SCHEMAS_FILE_ASYNC_REQUEST =
      SCHEMAS_FOLDER + "eblsurrender-v10-async-request.json";
  public static final String SCHEMAS_FILE_ASYNC_RESPONSE =
      SCHEMAS_FOLDER + "eblsurrender-v10-async-response.json";

  private final ScenarioListBuilder<EblSurrenderV10ScenarioListBuilder> scenarioListBuilder;

  public EblSurrenderV10ConformanceCheck(
      ScenarioListBuilder<EblSurrenderV10ScenarioListBuilder> scenarioListBuilder) {
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
                    if (EblSurrenderV10Role.isPlatform(exchange.getSourcePartyRole())) {
                      this.addResult(
                          ConformanceResult.forSourceParty(
                              exchange,
                              exchange.getRequestPath().endsWith("/v1/surrender-requests")));
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
                    if (EblSurrenderV10Role.isPlatform(exchange.getSourcePartyRole())) {
                      this.addResult(
                          ConformanceResult.forSourceParty(
                              exchange, jsonSchemaValidator.validate(exchange.getRequestBody())));
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
                    if (EblSurrenderV10Role.isPlatform(exchange.getSourcePartyRole())) {
                      this.addResult(
                          ConformanceResult.forTargetParty(
                              exchange, jsonSchemaValidator.validate(exchange.getResponseBody())));
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
                    if (EblSurrenderV10Role.isCarrier(exchange.getSourcePartyRole())) {
                      this.addResult(
                          ConformanceResult.forSourceParty(
                              exchange, jsonSchemaValidator.validate(exchange.getRequestBody())));
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
                    if (EblSurrenderV10Role.isCarrier(exchange.getSourcePartyRole())) {
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
        return "1.0.0".equals(apiVersion);
      }
    };
  }
}
