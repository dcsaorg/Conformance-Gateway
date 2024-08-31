package org.dcsa.conformance.core.check;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class ApiHeaderCheck extends ActionCheck {
  private final String expectedVersion;
  private final boolean isNotification;

  public static ApiHeaderCheck createNotificationCheck(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    String expectedVersion) {
    return new ApiHeaderCheck(
      "",
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      expectedVersion,
      true);
  }

  public static ApiHeaderCheck createNotificationCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String expectedVersion) {
    return new ApiHeaderCheck(
        titlePrefix,
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType,
        expectedVersion,
        true);
  }

  public ApiHeaderCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String expectedVersion) {
    this("", isRelevantForRoleName, matchedExchangeUuid, httpMessageType, expectedVersion, false);
  }

  public ApiHeaderCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String expectedVersion) {
    this(
        titlePrefix,
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType,
        expectedVersion,
        false);
  }

  private ApiHeaderCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String expectedVersion,
      boolean isNotification) {
    super(
        titlePrefix,
        "The HTTP %s has a correct Api-Version header"
            .formatted(httpMessageType.name().toLowerCase()),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType);
    this.expectedVersion = expectedVersion;
    this.isNotification = isNotification;
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Collections.emptySet();
    Map<String, ? extends Collection<String>> headers =
        exchange.getMessage(httpMessageType).headers();
    String headerName =
        headers.keySet().stream()
            .filter(key -> key.equalsIgnoreCase("api-version"))
            .findFirst()
            .orElse("api-version");
    Collection<String> headerValues = headers.get(headerName);
    if (headerValues == null || headerValues.isEmpty()) {
      return httpMessageType.equals(HttpMessageType.RESPONSE) && !isNotification
          ? Set.of("Missing Api-Version header")
          : Collections.emptySet();
    }
    if (headerValues.size() > 1) return Set.of("Duplicate Api-Version headers");
    String exchangeApiVersion = headerValues.stream().findFirst().orElseThrow();
    if (exchangeApiVersion.contains("-")) {
      exchangeApiVersion = exchangeApiVersion.substring(0, exchangeApiVersion.indexOf("-"));
    }
    return switch (httpMessageType) {
      case REQUEST -> isNotification
          ? _checkNotificationRequestApiVersionHeader(expectedVersion, exchangeApiVersion)
          : _checkRegularExchangeRequestApiVersionHeader(expectedVersion, exchangeApiVersion);
      case RESPONSE -> _checkResponseApiVersionHeader(expectedVersion, exchangeApiVersion);
    };
  }

  private Set<String> _checkRegularExchangeRequestApiVersionHeader(
      String standardApiVersion, String exchangeApiVersion) {
    return exchangeApiVersion == null
        ? Set.of()
        : _checkApiVersionHeaderValue(standardApiVersion.split("\\.")[0], exchangeApiVersion);
  }

  private Set<String> _checkNotificationRequestApiVersionHeader(
      String standardApiVersion, String exchangeApiVersion) {
    return _checkApiVersionHeaderValue(standardApiVersion, exchangeApiVersion);
  }

  private Set<String> _checkResponseApiVersionHeader(
      String standardApiVersion, String exchangeApiVersion) {
    return exchangeApiVersion == null
        ? Set.of()
        : _checkApiVersionHeaderValue(standardApiVersion, exchangeApiVersion);
  }

  private Set<String> _checkApiVersionHeaderValue(String expectedValue, String actualValue) {
    return Objects.equals(expectedValue, actualValue)
        ? Set.of()
        : Set.of("Expected Api-Version '%s' but found '%s'".formatted(expectedValue, actualValue));
  }
}
