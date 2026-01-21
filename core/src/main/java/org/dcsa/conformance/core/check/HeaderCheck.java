package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class HeaderCheck extends ActionCheck {

  private final String headerName;
  private final String headerValue;
  private final boolean checkPresenceOnly;

  /**
   * Constructor for checking only the presence of a header.
   * Does not validate the header value.
   */
  public HeaderCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String headerName) {
    this(
        "",
        "The header '%s' is present".formatted(headerName),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType,
        headerName,
        null,
        true);
  }

  /**
   * Constructor for checking both presence and value of a header.
   * Validates that the header exists and has the expected value.
   */
  public HeaderCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String headerName,
      String headerValue) {
    this(
        "",
        "The value of header '%s' is correct".formatted(headerName),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType,
        headerName,
        headerValue,
        false);
  }

  /**
   * Internal constructor with all parameters including title prefix and custom title.
   */
  private HeaderCheck(
      String titlePrefix,
      String title,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String headerName,
      String headerValue,
      boolean checkPresenceOnly) {
    super(
        titlePrefix,
        title,
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType);
    this.headerName = headerName;
    this.headerValue = headerValue;
    this.checkPresenceOnly = checkPresenceOnly;
  }

  @Override
  protected ConformanceCheckResult performCheck(
      Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return ConformanceCheckResult.simple(Set.of());

    var headers =
        httpMessageType == HttpMessageType.REQUEST
            ? exchange.getRequest().message().headers()
            : exchange.getResponse().message().headers();

    var values = headers.entrySet().stream()
        .filter(entry -> entry.getKey().equalsIgnoreCase(headerName))
        .map(java.util.Map.Entry::getValue)
        .findFirst()
        .orElse(null);

    // Check for header presence
    if (values == null || values.isEmpty()) {
      String errorMessage = checkPresenceOnly
          ? "Missing the header '%s'".formatted(headerName)
          : "Missing the header '%s' (which should be set to '%s')"
              .formatted(headerName, headerValue);
      return ConformanceCheckResult.simple(Set.of(errorMessage));
    }

    if (checkPresenceOnly) {
      return ConformanceCheckResult.simple(Collections.emptySet());
    }

    // Check that header appears exactly once
    if (values.size() != 1) {
      return ConformanceCheckResult.simple(
          Set.of("The header '%s' should be given exactly once".formatted(headerName)));
    }

    // Check header value matches expected value
    var actualValue = values.iterator().next();
    return ConformanceCheckResult.simple(
        headerValue.equals(actualValue)
            ? Collections.emptySet()
            : Set.of(
                "The header '%s' should have been '%s' but was '%s'"
                    .formatted(headerName, headerValue, actualValue)));
  }
}

