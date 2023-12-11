package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class JsonAttributePresenceCheck extends ActionCheck {
  private final JsonPointer jsonPointer;
  private final boolean mustBePresent;

  protected JsonAttributePresenceCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      JsonPointer jsonPointer,
      boolean mustBePresent) {
    super(
        titlePrefix,
        "The HTTP %s has a correct %s"
            .formatted(httpMessageType.name().toLowerCase(), renderJsonPointer(jsonPointer)),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType);
    this.jsonPointer = jsonPointer;
    this.mustBePresent = mustBePresent;
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
    var attributeNode = jsonBody.at(jsonPointer);
    if (this.mustBePresent) {
      if (attributeNode.isMissingNode()) {
        return Set.of(
          "The attribute '%s' should have been present, but was absent"
            .formatted(renderJsonPointer(jsonPointer)));
      }
      return Collections.emptySet();
    }
    // Note that "absent" is not the same as "present but null" according to the JSON schema definition.
    // This code attempts to mimic the "absent" definition for
    if (attributeNode.isMissingNode()) return Collections.emptySet();
    return Set.of(
        "The attribute '%s' should have been absent but was present and had value '%s'"
            .formatted(renderJsonPointer(jsonPointer), renderValue(attributeNode.asText(null))));
  }

  public static ActionCheck jsonAttributeMustBePresent(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonPointer attributePath
  ) {
    return jsonAttributeMustBePresent(
      "",
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      attributePath);
  }


  public static ActionCheck jsonAttributeMustBePresent(
    String titlePrefix,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonPointer jsonPointer
  ) {
    return new JsonAttributePresenceCheck(
      titlePrefix,
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      jsonPointer,
      true
    );
  }


  public static ActionCheck jsonAttributeMustBeAbsent(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonPointer attributePath
  ) {
    return jsonAttributeMustBeAbsent(
      "",
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      attributePath);
  }


  public static ActionCheck jsonAttributeMustBeAbsent(
    String titlePrefix,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonPointer jsonPointer
  ) {
    return new JsonAttributePresenceCheck(
      titlePrefix,
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      jsonPointer,
      false
    );
  }
}
