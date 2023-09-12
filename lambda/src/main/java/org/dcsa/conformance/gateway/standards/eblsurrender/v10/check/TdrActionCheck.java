package org.dcsa.conformance.gateway.standards.eblsurrender.v10.check;

import java.util.*;
import java.util.stream.Stream;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceResult;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

public abstract class TdrActionCheck extends ActionCheck {
  protected final int expectedStatus;

  public TdrActionCheck(String title, ActionCheck parent, int expectedStatus) {
    super(title, parent);
    this.expectedStatus = expectedStatus;
  }

  @Override
  protected void exchangeOccurred(ConformanceExchange exchange) {
    if (isRelevantExchange(exchange)) {
      Stream<LinkedList<ConformanceExchange>> parentRelevantExchangeListsStream =
          parent.relevantExchangeListsStream();
      if (parentRelevantExchangeListsStream == null) {
        addRelevantExchangeList(new LinkedList<>(List.of(exchange)));
      } else {
        parentRelevantExchangeListsStream
            .filter(parentExchangeList -> exchangeMatchesPreviousOnes(exchange, parentExchangeList))
            .forEach(
                parentExchangeList ->
                    addRelevantExchangeList(
                        new LinkedList<>(
                            Stream.concat(parentExchangeList.stream(), Stream.of(exchange))
                                .toList())));
      }
    }
  }

  protected abstract boolean isRelevantExchange(ConformanceExchange exchange);

  protected boolean exchangeMatchesPreviousOnes(
      ConformanceExchange exchange, LinkedList<ConformanceExchange> previousExchanges) {
    LinkedList<ConformanceExchange> reversedPreviousExchanges = new LinkedList<>(previousExchanges);
    Collections.reverse(reversedPreviousExchanges);
    return reversedPreviousExchanges.stream()
            .anyMatch(
                previousExchange -> exchangeMatchesPreviousRequest(exchange, previousExchange))
        || ( // support scenarios starting with a response rejected with 409
        this instanceof SurrenderRequestCheck
            && Stream.of("SURR", "SREJ")
                .anyMatch(
                    action ->
                        JsonToolkit.stringAttributeEquals(
                            Objects.requireNonNull(previousExchanges.peekLast())
                                .getRequest()
                                .message()
                                .body()
                                .getJsonBody(),
                            "action",
                            action)));
  }

  protected abstract boolean exchangeMatchesPreviousRequest(
      ConformanceExchange exchange, ConformanceExchange previousExchange);

  @Override
  protected void doCheck(ConformanceExchange exchange) {
    if (exchange
        == Optional.ofNullable(
                new LinkedList<>(TdrActionCheck.this.relevantExchangeListsStream().toList())
                    .peekLast())
            .orElse(new LinkedList<>())
            .peekLast()) {
      this.addResult(
          ConformanceResult.forTargetParty(
              exchange,
              Objects.requireNonNull(exchange).getResponse().statusCode() == expectedStatus));
    }
  }
}
