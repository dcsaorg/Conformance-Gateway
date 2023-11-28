package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class QueryParamCheck extends ActionCheck {
  private final String queryParamName;
  private final String queryParamValue;

  public QueryParamCheck(
      Predicate<String> isRelevantForRoleName, UUID matchedExchangeUuid, String queryParamName, String queryParamValue) {
    this("", isRelevantForRoleName, matchedExchangeUuid, queryParamName, queryParamValue);
  }

  public QueryParamCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String queryParamName,
      String queryParamValue) {
    super(
        titlePrefix,
        "The query param of the HTTP request is correct",
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST);
    this.queryParamName = queryParamName;
    this.queryParamValue = queryParamValue;
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Set.of();
    var queryParams = exchange.getRequest().queryParams();
    var values = queryParams.get(queryParamName);
    if (values == null || values.isEmpty()) {
      return Set.of("Missing the query parameter '%s' (which should be set to '%s')".formatted(queryParamName, queryParamValue));
    }
    if (values.size() != 1) {
      return Set.of("The query parameter '%s' should be given exactly once".formatted(queryParamName));
    }
    var actualValue = values.iterator().next();
    return queryParamValue.equals(actualValue)
        ? Collections.emptySet()
        : Set.of("The query parameter '%s' should have been '%s' but was '%s'".formatted(queryParamName, queryParamValue, actualValue));
  }
}
