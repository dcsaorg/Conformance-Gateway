package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class UrlPathCheck extends ActionCheck {
  private final String expectedUrlPathEnd;
  private final boolean allowAlternatePath;
  private final String alternateUrlPathEnd;

  public UrlPathCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String expectedUrlPathEnd) {
    this("", isRelevantForRoleName, matchedExchangeUuid, expectedUrlPathEnd, false, null);
  }

  public UrlPathCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String expectedUrlPathEnd) {
    this(titlePrefix, isRelevantForRoleName, matchedExchangeUuid, expectedUrlPathEnd, false, null);
  }

  public UrlPathCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String expectedUrlPathEnd,
      boolean allowAlternatePath,
      String alternateUrlPathEnd) {
    super(
        titlePrefix,
        "The URL path of the HTTP request is correct",
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST);
    this.expectedUrlPathEnd = expectedUrlPathEnd;
    this.allowAlternatePath = allowAlternatePath;
    this.alternateUrlPathEnd = alternateUrlPathEnd;
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Set.of();

    String requestUrl = exchange.getRequest().url();

    boolean matchesPrimary = requestUrl.endsWith(expectedUrlPathEnd);
    boolean matchesAlternate =
        allowAlternatePath
            && alternateUrlPathEnd != null
            && requestUrl.endsWith(alternateUrlPathEnd);

    return (matchesPrimary || matchesAlternate)
        ? Collections.emptySet()
        : Set.of(
            "Request URL '%s' does not end with '%s'%s"
                .formatted(
                    requestUrl,
                    expectedUrlPathEnd,
                    allowAlternatePath && alternateUrlPathEnd != null
                        ? " or '" + alternateUrlPathEnd + "'"
                        : ""));
  }
}
