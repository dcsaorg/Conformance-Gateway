package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class JsonAttributeCheck extends ActionCheck {
  private final List<String> attributePath;
  private final String expectedValue;

  public JsonAttributeCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      List<String> attributePath,
      String expectedValue) {
    this(
        "",
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType,
        attributePath,
        expectedValue);
  }

  public JsonAttributeCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      List<String> attributePath,
      String expectedValue) {
    super(
        titlePrefix,
        "The HTTP %s has a correct %s"
            .formatted(httpMessageType.name().toLowerCase(), attributePath),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType);
    this.attributePath = attributePath;
    this.expectedValue = expectedValue;
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Collections.emptySet();
    JsonNode jsonBody = exchange.getMessage(httpMessageType).body().getJsonBody();
    String actualValue = JsonToolkit.getTextAttributeOrNull(jsonBody, attributePath);
    if (expectedValue.equals(actualValue)) return Collections.emptySet();
    return Set.of(
        "The value of '%s' was '%s' instead of '%s'"
            .formatted(attributePath, actualValue, expectedValue));
  }
}
