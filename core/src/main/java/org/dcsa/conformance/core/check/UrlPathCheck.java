package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class UrlPathCheck extends ActionCheck {
  private final String expectedUrlPath;

  public UrlPathCheck(
      Predicate<String> isRelevantForRoleName, UUID matchedExchangeUuid, String expectedUrlPath) {
    super(
        "The URL path of the HTTP request is correct",
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST);
    this.expectedUrlPath = expectedUrlPath;
  }

  @Override
  protected Set<String> checkConformance(ConformanceExchange exchange) {
    String requestPath = exchange.getRequest().path();
    return requestPath.endsWith(expectedUrlPath)
        ? Collections.emptySet()
        : Set.of(
            "Request path '%s' does not end with '%s'".formatted(requestPath, expectedUrlPath));
  }
}
