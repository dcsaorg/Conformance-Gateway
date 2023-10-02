package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Predicate;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class JsonAttributeCheck extends ActionCheck {
  private final String attributeName;
  private final String expectedValue;

  public JsonAttributeCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      String attributeName,
      String expectedValue) {
    super(
        "The HTTP %s has a correct %s"
            .formatted(httpMessageType.name().toLowerCase(), attributeName),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType);
    this.attributeName = attributeName;
    this.expectedValue = expectedValue;
  }

  @Override
  protected Set<String> checkConformance(ConformanceExchange exchange) {
    JsonNode jsonBody = exchange.getMessage(httpMessageType).body().getJsonBody();
    String actualValue = JsonToolkit.getTextAttributeOrNull(jsonBody, attributeName);
    return expectedValue.equals(actualValue)
        ? Collections.emptySet()
        : Set.of(
            "The value of '%s' was '%s' instead of '%s'"
                .formatted(attributeName, actualValue, expectedValue));
  }
}
