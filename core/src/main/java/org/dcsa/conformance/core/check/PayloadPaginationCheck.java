package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class PayloadPaginationCheck extends ActionCheck {

  private final String firstPageHash;
  private final String secondPageHash;

  public PayloadPaginationCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String firstPageHash,
      String secondPageHash) {
    super(
        "The HTTP %s is paginated correctly".formatted(httpMessageType.getName()),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType);
    this.firstPageHash = firstPageHash;
    this.secondPageHash = secondPageHash;
  }

  @Override
  protected ConformanceCheckResult performCheck(
      Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) {
      return ConformanceCheckResult.simple(Collections.emptySet());
    }

    var issues = new LinkedHashSet<String>();

    if (firstPageHash == null || secondPageHash == null) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }

    if (Objects.equals(firstPageHash, secondPageHash)) {
      issues.add("The second page must be different from the first page");
    }

    return ConformanceCheckResult.simple(issues);
  }
}
