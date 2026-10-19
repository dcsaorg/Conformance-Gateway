package org.dcsa.conformance.core.check;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

/** Requires an empty response body when the configured HTTP status is returned. */
public class EmptyResponseBodyCheck extends ActionCheck {
  private final int applicableResponseStatus;

  public EmptyResponseBodyCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      int applicableResponseStatus) {
    this("", isRelevantForRoleName, matchedExchangeUuid, applicableResponseStatus);
  }

  public EmptyResponseBodyCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      int applicableResponseStatus) {
    super(
        titlePrefix,
        "The HTTP %d response has no payload".formatted(applicableResponseStatus),
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE);
    this.applicableResponseStatus = applicableResponseStatus;
  }

  @Override
  protected ConformanceCheckResult performCheck(
      Function<UUID, ConformanceExchange> getExchangeByUuid) {
    var exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null || exchange.getResponse().statusCode() != applicableResponseStatus) {
      return ConformanceCheckResult.simple(Set.of());
    }
    return ConformanceCheckResult.simple(
        exchange.getResponse().message().body().getStringBody().isEmpty()
            ? Set.of()
            : Set.of(
                "A %d response must not contain a payload.".formatted(applicableResponseStatus)));
  }
}
