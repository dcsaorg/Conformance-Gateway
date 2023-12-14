package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.NonNull;
import org.dcsa.conformance.core.traffic.HttpMessageType;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class JsonAttribute {

  public static ActionCheck contentChecks(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonContentCheck ... checks
  ) {
    return contentChecks("", isRelevantForRoleName, matchedExchangeUuid, httpMessageType, Arrays.asList(checks));
  }

  public static ActionCheck contentChecks(
    String titlePrefix,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    JsonContentCheck ... checks
  ) {
    return contentChecks(titlePrefix, isRelevantForRoleName, matchedExchangeUuid, httpMessageType, Arrays.asList(checks));
  }


  public static ActionCheck contentChecks(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    List<JsonContentCheck> checks
  ) {
    return contentChecks("", isRelevantForRoleName, matchedExchangeUuid, httpMessageType, checks);
  }

  public static ActionCheck contentChecks(
    String titlePrefix,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    List<JsonContentCheck> checks
  ) {
    return new JsonAttributeBasedCheck(
      titlePrefix,
      "The HTTP %s has valid content (conditional validation rules)"
        .formatted(httpMessageType.name().toLowerCase()),
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      checks
    );
  }

  public static JsonContentCheck mustEqual(
      JsonPointer jsonPointer,
      String expectedValue) {
    Objects.requireNonNull(
      expectedValue,
      "expectedValue cannot be null; Note: Use `() -> getDspSupplier().get().foo()` (or similar) when testing a value against a dynamic scenario property"
    );
    return new JsonContentCheckImpl(
      jsonCheckName(jsonPointer),
      at(jsonPointer, node -> {
        var actualValue = node.asText(null);
        if (!Objects.equals(expectedValue, actualValue)) {
          return Set.of(
            "The value of '%s' was '%s' instead of '%s'"
              .formatted(renderJsonPointer(jsonPointer), renderValue(actualValue), renderValue(expectedValue)));
        }
        return Collections.emptySet();
      }
    ));
  }

  public static JsonContentCheck mustEqual(
    JsonPointer jsonPointer,
    @NonNull
    Supplier<String> expectedValueSupplier) {
    return new JsonContentCheckImpl(
      jsonCheckName(jsonPointer),
      at(jsonPointer, node -> {
          var actualValue = node.asText(null);
          var expectedValue = expectedValueSupplier.get();
          if (expectedValue == null) {
            throw new IllegalStateException("The supplier of the expected value for " + renderJsonPointer(jsonPointer)
              + " returned `null` and `null` is not supported for equals. Usually this indicates that the dynamic"
              + " scenario property was not properly recorded at this stage.");
          }
          if (!Objects.equals(expectedValue, actualValue)) {
            return Set.of(
              "The value of '%s' was '%s' instead of '%s'"
                .formatted(renderJsonPointer(jsonPointer), renderValue(actualValue), renderValue(expectedValue)));
          }
          return Collections.emptySet();
        }
      ));
  }

  public static JsonContentCheck mustBePresent(JsonPointer jsonPointer) {
    return new JsonContentCheckImpl(
      jsonCheckName(jsonPointer),
      at(jsonPointer, node -> {
        if (node.isMissingNode()) {
          return Set.of(
            "The attribute '%s' should have been present but was absent"
              .formatted(renderJsonPointer(jsonPointer)));
        }
        return Collections.emptySet();
      }));
  }

  public static JsonContentCheck mustBeAbsent(
    JsonPointer jsonPointer
  ) {
    return new JsonContentCheckImpl(
        jsonCheckName(jsonPointer),
        at(jsonPointer, node -> {
          if (!node.isMissingNode()) {
            return Set.of(
              "The attribute '%s' should have been absent but was present and had value '%s'"
                .formatted(renderJsonPointer(jsonPointer), renderValue(node.asText(null))));
          }
          return Collections.emptySet();
        }));
  }

  public static JsonContentCheck mustBeDatasetKeyword(
    JsonPointer jsonPointer,
    KeywordDataset dataset
  ) {
    return new JsonContentCheckImpl(
      jsonCheckName(jsonPointer),
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

  private static String jsonCheckName(JsonPointer jsonPointer) {
    return "The JSON body has a correct %s"
      .formatted(renderJsonPointer(jsonPointer));
  }


  record JsonContentCheckImpl(
    @NonNull
    String description,
    @NonNull
    Function<JsonNode, Set<String>> impl
  ) implements JsonContentCheck {
    @Override
    public Set<String> validate(JsonNode body) {
      return impl.apply(body);
    }
  }
}
