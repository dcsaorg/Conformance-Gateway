package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class UrlPathCheck extends ActionCheck {

  private final Set<String> expectedUrlPathEnd;

  public UrlPathCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String... expectedUrlPathEnd) {
    this("", isRelevantForRoleName, matchedExchangeUuid, expectedUrlPathEnd);
  }

  public UrlPathCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String... expectedUrlPathEnd) {
    super(
        titlePrefix,
        "The URL path of the HTTP request is correct",
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST);
    this.expectedUrlPathEnd = Set.of(expectedUrlPathEnd);
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Set.of();

    String requestUrl = exchange.getRequest().url();

    boolean matches = expectedUrlPathEnd.stream().anyMatch(requestUrl::endsWith);

    return matches
        ? Collections.emptySet()
        : Set.of(
            "Request URL '%s' does not end with any of %s"
                .formatted(requestUrl, expectedUrlPathEnd));
  }
}
