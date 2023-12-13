package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.traffic.HttpMessageType;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public class JsonAttribute {

  public static ActionCheck mustEqual(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonPointer jsonPointer,
    String value) {
    return mustEqual(
      "",
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      jsonPointer,
      value);
  }


  public static ActionCheck mustEqual(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      JsonPointer jsonPointer,
      String expectedValue) {
    return new JsonAttributeBasedCheck(
      titlePrefix,
      actionTitle(httpMessageType, jsonPointer),
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      at(jsonPointer, node -> {
        var actualValue = node.asText(null);
        if (!Objects.equals(expectedValue, actualValue)) {
          return Set.of(
            "The value of '%s' was '%s' instead of '%s'"
              .formatted(renderJsonPointer(jsonPointer), renderValue(actualValue), renderValue(expectedValue)));
        }
        return Collections.emptySet();
      }));
  }


  public static ActionCheck mustBePresent(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonPointer attributePath
  ) {
    return mustBePresent(
      "",
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      attributePath);
  }


  public static ActionCheck mustBePresent(
    String titlePrefix,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonPointer jsonPointer
  ) {
    return new JsonAttributeBasedCheck(
      titlePrefix,
      actionTitle(httpMessageType, jsonPointer),
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      at(jsonPointer, node -> {
        if (node.isMissingNode()) {
          return Set.of(
            "The attribute '%s' should have been present but was absent"
              .formatted(renderJsonPointer(jsonPointer)));
        }
        return Collections.emptySet();
      }));
  }


  public static ActionCheck mustBeAbsent(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonPointer attributePath
  ) {
    return mustBeAbsent(
      "",
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      attributePath);
  }


  public static ActionCheck mustBeAbsent(
    String titlePrefix,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonPointer jsonPointer
  ) {
    return new JsonAttributeBasedCheck(
        titlePrefix,
        actionTitle(httpMessageType, jsonPointer),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType,
        at(jsonPointer, node -> {
          if (!node.isMissingNode()) {
            return Set.of(
              "The attribute '%s' should have been absent but was present and had value '%s'"
                .formatted(renderJsonPointer(jsonPointer), renderValue(node.asText(null))));
          }
          return Collections.emptySet();
        }));
  }

  public static ActionCheck mustBeDatasetKeyword(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonPointer jsonPointer,
    KeywordDataset dataset
  ) {
    return new JsonAttributeBasedCheck(
      "",
      actionTitle(httpMessageType, jsonPointer),
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      at(jsonPointer, node -> {
        var text = node.asText();
        if (!dataset.contains(text)) {
          return Set.of(
            "The attribute '%s' had value '%s' which was not a valid keyword here."
              .formatted(renderJsonPointer(jsonPointer), renderValue(node.asText(null))));
        }
        return Collections.emptySet();
      }));
  }

  private static Function<JsonNode, JsonNode> at(JsonPointer jsonPointer) {
    return (refNode) -> refNode.at(jsonPointer);
  }

  private static Function<JsonNode,  Set<String>> at(JsonPointer jsonPointer, Function<JsonNode, Set<String>> validator) {
    return at(jsonPointer).andThen(validator);
  }

  static String renderValue(String v) {
    return v == null ? "(null)" : v;
  }

  static String renderJsonPointer(JsonPointer jsonPointer) {
    return jsonPointer.toString().substring(1).replace("/", ".");
  }

  private static String actionTitle(HttpMessageType httpMessageType, JsonPointer jsonPointer){
    return "The HTTP %s has a correct %s"
      .formatted(httpMessageType.name().toLowerCase(), renderJsonPointer(jsonPointer));
  }
}
