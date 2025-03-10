package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class HttpMethodCheck extends ActionCheck {
  private final String expectedHttpMethod;

  public HttpMethodCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String expectedHttpMethod) {
    this("", isRelevantForRoleName, matchedExchangeUuid, expectedHttpMethod);
  }

  public HttpMethodCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String expectedHttpMethod) {
    super(
        titlePrefix,
        "The HTTP request method is correct",
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST);
    this.expectedHttpMethod = expectedHttpMethod;
  }

  @Override
  public Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Set.of();
    var method = exchange.getRequest().method();
    return method.equals(expectedHttpMethod)
        ? Collections.emptySet()
        : Set.of(
            "Request HTTP method was '%s' but should have been '%s'"
                .formatted(method, expectedHttpMethod));
  }
}
