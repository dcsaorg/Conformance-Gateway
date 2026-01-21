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
  private final boolean checkPresenceOnly;

  /**
   * Constructor for checking only the presence of a query parameter.
   * Does not validate the parameter value.
   */
  public QueryParamCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String queryParamName) {
    this(
        "",
        "The query parameter '%s' is present".formatted(queryParamName),
        isRelevantForRoleName,
        matchedExchangeUuid,
        queryParamName,
        null,
        true);
  }

  /**
   * Constructor for checking both presence and value of a query parameter.
   * Validates that the parameter exists and has the expected value.
   */
  public QueryParamCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String queryParamName,
      String queryParamValue) {
    this(
        "",
        "The value of query parameter '%s' is correct".formatted(queryParamName),
        isRelevantForRoleName,
        matchedExchangeUuid,
        queryParamName,
        queryParamValue,
        false);
  }

  /**
   * Internal constructor with all parameters including title prefix and custom title.
   */
  private QueryParamCheck(
      String titlePrefix,
      String title,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      String queryParamName,
      String queryParamValue,
      boolean checkPresenceOnly) {
    super(
        titlePrefix,
        title,
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST);
    this.queryParamName = queryParamName;
    this.queryParamValue = queryParamValue;
    this.checkPresenceOnly = checkPresenceOnly;
  }

  @Override
  protected ConformanceCheckResult performCheck(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return ConformanceCheckResult.simple(Set.of());

    var queryParams = exchange.getRequest().queryParams();
    var values = queryParams.get(queryParamName);

    // Check for query parameter presence
    if (values == null || values.isEmpty()) {
      String errorMessage = checkPresenceOnly
          ? "Missing the query parameter '%s'".formatted(queryParamName)
          : "Missing the query parameter '%s' (which should be set to '%s')"
              .formatted(queryParamName, queryParamValue);
      return ConformanceCheckResult.simple(Set.of(errorMessage));
    }

    // If only checking presence, we're done
    if (checkPresenceOnly) {
      return ConformanceCheckResult.simple(Collections.emptySet());
    }

    // Check that parameter appears exactly once
    if (values.size() != 1) {
      return ConformanceCheckResult.simple(
          Set.of("The query parameter '%s' should be given exactly once".formatted(queryParamName)));
    }

    // Check parameter value matches expected value
    var actualValue = values.iterator().next();
    return ConformanceCheckResult.simple(queryParamValue.equals(actualValue)
        ? Collections.emptySet()
        : Set.of("The query parameter '%s' should have been '%s' but was '%s'"
            .formatted(queryParamName, queryParamValue, actualValue)));
  }
}
