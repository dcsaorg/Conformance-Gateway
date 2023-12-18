package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.dcsa.conformance.core.traffic.HttpMessageType;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

  public static Predicate<JsonNode> isTrue(
    @NonNull
    JsonPointer jsonPointer
  ) {
    return isTrue(jsonPointer, false);
  }

  public static Predicate<JsonNode> isTrue(
    @NonNull
    JsonPointer jsonPointer,
    boolean defaultValue
  ) {
    return (root) -> at(jsonPointer).apply(root).asBoolean(defaultValue);
  }


  public static Predicate<JsonNode> isNotNull(
    @NonNull
    JsonPointer jsonPointer
  ) {
    return (root) -> {
      var node = at(jsonPointer).apply(root);
      return !node.isMissingNode() && !node.isNull();
    };
  }

  public static JsonContentCheck mustBeTrue(
    JsonPointer jsonPointer
  ) {
     return new JsonContentCheckImpl(
      jsonCheckName(jsonPointer),
      at(jsonPointer, node -> {
          if (!node.isBoolean() || !node.asBoolean(false)) {
            return Set.of(
              "The value of '%s' must be true (boolean) but was '%s'"
                .formatted(renderJsonPointer(jsonPointer), renderValue(node.asText(null))));
          }
          return Collections.emptySet();
        }
      ));
  }

  public static JsonContentCheck mustBeFalse(
    JsonPointer jsonPointer
  ) {
    return new JsonContentCheckImpl(
      jsonCheckName(jsonPointer),
      at(jsonPointer, node -> {
          if (!node.isBoolean() || !node.asBoolean(true)) {
            return Set.of(
              "The value of '%s' must be false (boolean) but was '%s'"
                .formatted(renderJsonPointer(jsonPointer), renderValue(node.asText(null))));
          }
          return Collections.emptySet();
        }
      ));
  }

  public static JsonContentCheck mustBeNotNull(
    JsonPointer jsonPointer,
    String reason
  ) {
    return new JsonContentCheckImpl(
      jsonCheckName(jsonPointer),
      at(jsonPointer, node -> {
          if (node.isMissingNode() || node.isNull()) {
            return Set.of(
              "The value of '%s' must present and not null because %s"
                .formatted(renderJsonPointer(jsonPointer), reason));
          }
          return Collections.emptySet();
        }
      ));
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

  public static JsonContentCheck mustBeDatasetKeywordIfPresent(
    JsonPointer jsonPointer,
    KeywordDataset dataset
  ) {
    return new JsonContentCheckImpl(
      jsonCheckName(jsonPointer),
      at(jsonPointer, node -> {
        var text = node.asText();
        // We rely on schema validation (or mustBePresent) for required check.
        if (!node.isMissingNode() && !dataset.contains(text)) {
          return Set.of(
            "The attribute '%s' had value '%s' which was not a valid keyword here."
              .formatted(renderJsonPointer(jsonPointer), renderValue(node.asText(null))));
        }
        return Collections.emptySet();
      }));
  }

  public static JsonContentCheck mutuallyExclusive(
    @NonNull JsonPointer ... ptrs
  ) {
    if (ptrs.length < 2) {
      throw new IllegalStateException("At least two arguments are required");
    }
    String name = "The following are mutually exclusive (at most one of): %s".formatted(
      Arrays.stream(ptrs)
        .map(JsonAttribute::renderJsonPointer)
        .collect(Collectors.joining(", "))
    );
    return new JsonContentCheckImpl(
      name,
      (body) -> {
        var present = Arrays.stream(ptrs)
          .filter(p -> isJsonNodePresent(body.at(p)))
          .toList();
        if (present.size() < 2) {
          return Set.of();
        }
        return Set.of(
          "At most one of the following can be present: %s".formatted(
            present.stream()
              .map(JsonAttribute::renderJsonPointer)
              .collect(Collectors.joining(", "
              ))
        ));
      });
  }

  public static JsonContentCheck allOrNoneArePresent(
    @NonNull JsonPointer ... ptrs
  ) {
    if (ptrs.length < 2) {
      throw new IllegalStateException("At least two arguments are required");
    }
    String name = "All or none of the following are present: %s".formatted(
      Arrays.stream(ptrs)
        .map(JsonAttribute::renderJsonPointer)
        .collect(Collectors.joining(", "))
    );
    return new JsonContentCheckImpl(
      name,
      (body) -> {
        var firstPtr = ptrs[0];
        var firstNode = body.at(firstPtr);
        Predicate<JsonNode> check;
        if (firstNode.isMissingNode() || firstNode.isNull()) {
          check = JsonAttribute::isJsonNodePresent;
        } else {
          check = JsonAttribute::isJsonNodeAbsent;
        }
        var conflictingPtr = Arrays.stream(ptrs)
          .filter(p -> check.test(body.at(p)))
          .findAny()
          .orElse(null);
        if (conflictingPtr != null) {
          return Set.of("'%s' and '%s' must both be present or absent".formatted(
            renderJsonPointer(firstPtr), renderJsonPointer(conflictingPtr))
          );
        }
        return Set.of();
      });
  }

  public static JsonContentCheck ifThen(
    @NonNull
    String name,
    @NonNull
    Predicate<JsonNode> when,
    @NonNull
    JsonContentCheck then
  ) {
    return new JsonContentCheckImpl(
      name,
      (body) -> {
        if (when.test(body)) {
          return then.validate(body);
        }
        return Set.of();
      });
  }

  public static JsonContentCheck ifThenElse(
    @NonNull
    String name,
    @NonNull
    Predicate<JsonNode> when,
    @NonNull
    JsonContentCheck then,
    @NonNull
    JsonContentCheck elseCheck
  ) {
    return new JsonContentCheckImpl(
      name,
      (body) -> {
        if (when.test(body)) {
          return then.validate(body);
        }
        return elseCheck.validate(body);
      });
  }

  private static Function<JsonNode, JsonNode> at(JsonPointer jsonPointer) {
    return (refNode) -> refNode.at(jsonPointer);
  }

  private static Function<JsonNode, Set<String>> at(JsonPointer jsonPointer, Function<JsonNode, Set<String>> validator) {
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

  private static boolean isJsonNodePresent(JsonNode node) {
    return !node.isMissingNode() && !node.isNull();
  }

  private static boolean isJsonNodeAbsent(JsonNode node) {
    return !isJsonNodePresent(node);
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
