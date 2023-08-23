package org.dcsa.conformance.gateway.standards.eblsurrender.v10.check;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.dcsa.conformance.gateway.check.ActionCheck;
import org.dcsa.conformance.gateway.check.ConformanceCheck;
import org.dcsa.conformance.gateway.check.ConformanceResult;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class TdrActionCheck extends ActionCheck {
  protected final int expectedStatus;
  protected final Predicate<String> responseHttpStatusCheckRolePredicate;


  public TdrActionCheck(String title, ActionCheck parent, int expectedStatus, Predicate<String> responseHttpStatusCheckRolePredicate) {
    super(title, parent);
    this.expectedStatus = expectedStatus;
    this.responseHttpStatusCheckRolePredicate = responseHttpStatusCheckRolePredicate;
  }

  protected static String getTdr(JsonNode jsonRequest) {
    return jsonRequest.get("transportDocumentReference").asText();
  }

  @SneakyThrows
  protected static JsonNode getJsonRequest(ConformanceExchange exchange) {
    return new ObjectMapper().readTree(exchange.getRequestBody());
  }

  @Override
  protected void exchangeOccurred(ConformanceExchange exchange) {
    JsonNode jsonRequest = getJsonRequest(exchange);
    if (isRelevantRequestType(jsonRequest)) {
      String tdr = getTdr(jsonRequest);
      Stream<LinkedList<ConformanceExchange>> parentRelevantExchangeListsStream =
          parent.relevantExchangeListsStream();
      if (parentRelevantExchangeListsStream == null) {
        addRelevantExchangeList(new LinkedList<>(List.of(exchange)));
      } else {
        parentRelevantExchangeListsStream
            .filter(
                parentExchangeList ->
                    Objects.equals(
                        tdr,
                        getTdr(
                            getJsonRequest(Objects.requireNonNull(parentExchangeList.peekLast())))))
            .forEach(
                parentExchangeList ->
                    addRelevantExchangeList(
                        new LinkedList<>(
                            Stream.concat(parentExchangeList.stream(), Stream.of(exchange))
                                .toList())));
      }
    }
  }

  protected abstract boolean isRelevantRequestType(JsonNode jsonRequest);

  protected ConformanceExchange latestRelevantExchange() {
    return Optional.ofNullable(
                    new LinkedList<>(
                            TdrActionCheck.this
                                    .relevantExchangeListsStream()
                                    .toList())
                            .peekLast())
            .orElse(new LinkedList<>())
            .peekLast();
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.concat(
        Stream.of(
            new ConformanceCheck("HTTP response status is %d".formatted(expectedStatus)) {
              @Override
              public boolean isRelevantForRole(String roleName) {
                return responseHttpStatusCheckRolePredicate.test(roleName);
              }

              @Override
              protected void doCheck(ConformanceExchange exchange) {
                if (responseHttpStatusCheckRolePredicate.test(exchange.getTargetPartyRole())) {
                  if (exchange == latestRelevantExchange()) {
                    this.addResult(
                        ConformanceResult.forTargetParty(
                            exchange, exchange.getResponseStatusCode() == expectedStatus));
                  }
                }
              }
            }),
        super.createSubChecks());
  }
}
