package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ResponseLimitCheck extends ActionCheck {

  private final Supplier<String> limitSupplier;
  private final String rootObjectLabel;
  private final String arrayFieldName;

  /**
   * Constructs a ResponseLimitCheck that validates against the root response array.
   */
  public ResponseLimitCheck(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    @NonNull Supplier<String> limitSupplier,
    @NonNull String rootObjectLabel) {
    this(isRelevantForRoleName, matchedExchangeUuid, httpMessageType, limitSupplier, rootObjectLabel, null);
  }

  /**
   * Constructs a ResponseLimitCheck that validates against a named array field in the response body.
   *
   * @param arrayFieldName the name of the array field to check, or {@code null} to check the root array
   */
  public ResponseLimitCheck(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    @NonNull Supplier<String> limitSupplier,
    @NonNull String rootObjectLabel,
    String arrayFieldName) {
    super(
      "The HTTP response body does not exceed the supplied limit",
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType);
    this.limitSupplier = limitSupplier;
    this.rootObjectLabel = rootObjectLabel;
    this.arrayFieldName = arrayFieldName;
  }

  @Override
  protected ConformanceCheckResult performCheck(
    Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) {
      return ConformanceCheckResult.simple(Set.of());
    }

    String suppliedLimit = limitSupplier.get();
    if (suppliedLimit == null || suppliedLimit.isBlank()) {
      setApplicable(false);
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }

    final int limit;
    try {
      limit = Integer.parseInt(suppliedLimit);
    } catch (NumberFormatException e) {
      return ConformanceCheckResult.simple(Set.of("The supplied limit '%s' is not a valid integer.".formatted(suppliedLimit)));
    }

    JsonNode body = exchange.getMessage(httpMessageType).body().getJsonBody();
    JsonNode arrayToCheck;

    if (arrayFieldName != null) {
      arrayToCheck = body.path(arrayFieldName);
      if (arrayToCheck.isMissingNode() || !arrayToCheck.isArray()) {
        return ConformanceCheckResult.simple(Set.of(
          "The response body field '%s' must be a JSON array to validate pagination limits.".formatted(arrayFieldName)));
      }
    } else {
      if (!body.isArray()) {
        return ConformanceCheckResult.simple(Set.of("The response body must be a root JSON array to validate pagination limits."));
      }
      arrayToCheck = body;
    }

    int actualCount = arrayToCheck.size();
    if (actualCount > limit) {
      return ConformanceCheckResult.simple(Set.of("The response contained %d %s object(s), which exceeds the supplied limit of %d."
        .formatted(actualCount, rootObjectLabel, limit)));
    }

    return ConformanceCheckResult.simple(Set.of());
  }
}
