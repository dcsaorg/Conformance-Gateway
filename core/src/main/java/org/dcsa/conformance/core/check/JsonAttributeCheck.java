package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class JsonAttributeCheck extends ActionCheck {
  private final JsonPointer jsonPointer;
  private final String expectedValue;

  public JsonAttributeCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      JsonPointer attributePath,
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
      JsonPointer jsonPointer,
      String expectedValue) {
    super(
        titlePrefix,
        "The HTTP %s has a correct %s"
            .formatted(httpMessageType.name().toLowerCase(), renderJsonPointer(jsonPointer)),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType);
    this.jsonPointer = jsonPointer;
    this.expectedValue = expectedValue;
  }

  private static String renderValue(String v) {
    return v == null ? "(null)" : v;
  }

  private static String renderJsonPointer(JsonPointer jsonPointer) {
    return jsonPointer.toString().substring(1).replace("/", ".");
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Collections.emptySet();
    JsonNode jsonBody = exchange.getMessage(httpMessageType).body().getJsonBody();
    var actualValue = jsonBody.at(jsonPointer).asText(null);
    if (expectedValue.equals(actualValue)) return Collections.emptySet();
    return Set.of(
        "The value of '%s' was '%s' instead of '%s'"
            .formatted(renderJsonPointer(jsonPointer), renderValue(actualValue), renderValue(expectedValue)));
  }
}
