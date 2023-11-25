package org.dcsa.conformance.core.check;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class ApiHeaderCheck extends ActionCheck {
  private final String expectedVersion;

  public ApiHeaderCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String expectedVersion) {
    this("", isRelevantForRoleName, matchedExchangeUuid, httpMessageType, expectedVersion);
  }

  public ApiHeaderCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String expectedVersion) {
    super(
        titlePrefix,
        "The HTTP %s has a correct Api-Version header"
            .formatted(httpMessageType.name().toLowerCase()),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType);
    this.expectedVersion = expectedVersion;
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
    if (headerValues == null || headerValues.isEmpty()) return Set.of("Missing Api-Version header");
    if (headerValues.size() != 1) return Set.of("Duplicate Api-Version headers");
    String apiVersion = headerValues.stream().findFirst().orElseThrow();
    return expectedVersion.equals(apiVersion)
        ? Collections.emptySet()
        : Set.of("Expected Api-Version '%s' but found '%s'".formatted(expectedVersion, apiVersion));
  }
}
