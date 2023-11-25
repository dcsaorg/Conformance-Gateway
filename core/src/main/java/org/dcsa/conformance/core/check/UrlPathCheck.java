package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class UrlPathCheck extends ActionCheck {
  private final String expectedUrlPath;

  public UrlPathCheck(
      Predicate<String> isRelevantForRoleName, UUID matchedExchangeUuid, String expectedUrlPath) {
    this("", isRelevantForRoleName, matchedExchangeUuid, expectedUrlPath);
  }

  public UrlPathCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String expectedUrlPath) {
    super(
        titlePrefix,
        "The URL path of the HTTP request is correct",
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST);
    this.expectedUrlPath = expectedUrlPath;
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Set.of();
    String requestUrl = exchange.getRequest().url();
    return requestUrl.endsWith(expectedUrlPath)
        ? Collections.emptySet()
        : Set.of("Request URL '%s' does not end with '%s'".formatted(requestUrl, expectedUrlPath));
  }
}
