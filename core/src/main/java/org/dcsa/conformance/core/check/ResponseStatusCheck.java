package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class ResponseStatusCheck extends ActionCheck {
  private final int expectedResponseStatus;

  public ResponseStatusCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      int expectedResponseStatus) {
    this("", isRelevantForRoleName, matchedExchangeUuid, expectedResponseStatus);
  }

  public ResponseStatusCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      int expectedResponseStatus) {
    super(
        titlePrefix,
        "The HTTP response status is correct",
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE);
    this.expectedResponseStatus = expectedResponseStatus;
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Collections.emptySet();
    int responseStatus = exchange.getResponse().statusCode();
    return responseStatus == expectedResponseStatus
        ? Collections.emptySet()
        : Set.of(
            "Response status '%d' does not match the expected value '%d'"
                .formatted(responseStatus, expectedResponseStatus));
  }
}
