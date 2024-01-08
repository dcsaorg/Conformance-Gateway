package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class JsonAttribute {

  private static final JsonPointer ROOT_PTR = JsonPointer.compile("/");

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
    return (baseNode) -> baseNode.at(jsonPointer).asBoolean(defaultValue);
  }

  public static Predicate<JsonNode> isTrue(
    @NonNull
    String path
  ) {
    return isTrue(path, false);
  }

  public static Predicate<JsonNode> isTrue(
    @NonNull
    String path,
    boolean defaultValue
  ) {
    return (baseNode) -> baseNode.path(path).asBoolean(defaultValue);
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

  public static JsonContentMatchedValidation presenceImpliesOtherField(
    @NonNull
    String sourceFieldName,
    @NonNull
    String impliedFieldName
  ) {
    return (JsonNode nodeToValidate, String contextPath) -> {
      var sourceField = nodeToValidate.path(sourceFieldName);
      var impliedField = nodeToValidate.path(impliedFieldName);
      if (sourceField.isMissingNode() || !impliedField.isMissingNode()) {
        return Set.of();
      }

      return Set.of("The field '%s.%s' being present makes '%s.%s' mandatory".formatted(
        contextPath,
        sourceFieldName,
        contextPath,
        impliedFieldName
      ));
    };
  }
  public static JsonContentCheck allIndividualMatchesMustBeValid(
    @NonNull
    String name,
    @NonNull
    Consumer<MultiAttributeValidator> scanner,
    @NonNull
    JsonContentMatchedValidation subvalidation
  ) {
    return JsonContentCheckImpl.of(
      name,
      (body) -> {
        var v = new MultiAttributeValidatorImpl(body, subvalidation);
        scanner.accept(v);
        return v.getValidationIssues();
      }
    );
  }

  public static JsonContentMatchedValidation unique(
    String field
  ) {
    return unique(
      "field %s with value".formatted(field),
      (node) -> node.path(field).asText(null)
    );
  }

  public static JsonContentMatchedValidation unique(
    String fieldA,
    String fieldB
  ) {
    return unique(
      "combination of %s/%s with value".formatted(fieldA, fieldB),
      (node) -> {
        var valueA = node.path(fieldA).asText(null);
        var valueB = node.path(fieldB).asText(null);
        if (valueA == null || valueB == null) {
          return null;
        }
        return valueA + "/" + valueB;
      }
    );
  }

  public static JsonContentMatchedValidation unique(
    String keyDescription,
    Function<JsonNode, String> keyFunction
  ) {
    return (array, contextPath) -> {
      var seen = new HashSet<String>();
      var duplicates = new LinkedHashSet<String>();
      for (var node : array) {
        var key = keyFunction.apply(node);
        if (key == null) {
          continue;
        }
        if (!seen.add(key)) {
          duplicates.add(key);
        }
      }
      return duplicates.stream()
        .map(dup -> "The %s '%s' must be unique but was used more than once in '%s'".formatted(keyDescription, dup, contextPath))
        .collect(Collectors.toSet());
    };
  }

  public static JsonContentMatchedValidation path(String path, JsonContentMatchedValidation delegate) {
    return (nodeToValidate, contextPath) -> {
      var fullContext = contextPath.isEmpty() ? path : contextPath + "." + path;
      return delegate.validate(nodeToValidate.path(path), fullContext);
    };
  }

  public static JsonContentMatchedValidation at(JsonPointer pointer, JsonContentMatchedValidation delegate) {
    String ptrAsContext = renderJsonPointer(pointer);
    return (nodeToValidate, contextPath) -> {
      var fullContext = contextPath.isEmpty() ? ptrAsContext : contextPath + "." + ptrAsContext;
      return delegate.validate(nodeToValidate.at(pointer), fullContext);
    };
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
      "%s: Must equal '%s'".formatted(jsonCheckName(jsonPointer), expectedValue),
      at(jsonPointer, node -> {
        var actualValue = node.asText(null);
        if (!Objects.equals(expectedValue, actualValue)) {
          return Set.of(
            "The value of '%s' was '%s' instead of '%s'"
              .formatted(renderJsonPointer(jsonPointer), renderValue(node), renderValue(expectedValue)));
        }
        return Collections.emptySet();
      }
    ));
  }

  public static JsonContentCheck mustEqual(
    JsonPointer jsonPointer,
    @NonNull
    Supplier<String> expectedValueSupplier) {
    return mustEqual(
      jsonCheckName(jsonPointer),
      jsonPointer,
      expectedValueSupplier
    );
  }


  public static JsonContentCheck mustEqual(
    String name,
    JsonPointer jsonPointer,
    @NonNull
    Supplier<String> expectedValueSupplier) {
    var v = expectedValueSupplier.get();
    var context = "";
    if (v != null) {
      context = ": Must equal '%s'".formatted(v);
    }
    return new JsonContentCheckImpl(
      name + context,
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
                .formatted(renderJsonPointer(jsonPointer), renderValue(node), renderValue(expectedValue)));
          }
          return Collections.emptySet();
        }
      ));
  }


  public static JsonContentCheck mustEqual(
    String name,
    String path,
    @NonNull
    Supplier<String> expectedValueSupplier) {
    var v = expectedValueSupplier.get();
    var context = "";
    if (v != null) {
      context = ": Must equal '%s'".formatted(v);
    }
    return new JsonContentCheckImpl(
      name + context,
      path(path, node -> {
          var actualValue = node.asText(null);
          var expectedValue = expectedValueSupplier.get();
          if (expectedValue == null) {
            throw new IllegalStateException("The supplier of the expected value for " + path
              + " returned `null` and `null` is not supported for equals. Usually this indicates that the dynamic"
              + " scenario property was not properly recorded at this stage.");
          }
          if (!Objects.equals(expectedValue, actualValue)) {
            return Set.of(
              "The value of '%s' was '%s' instead of '%s'"
                .formatted(path, renderValue(node), renderValue(expectedValue)));
          }
          return Collections.emptySet();
        }
      ));
  }

  public static JsonContentMatchedValidation matchedMustBePresent() {
    return (node, context) -> {
        if (node.isMissingNode()) {
          return Set.of(
            "The attribute '%s' should have been present but was absent"
              .formatted(context));
        }
        return Collections.emptySet();
      };
  }

  public static JsonContentMatchedValidation matchedMustEqual(Supplier<String> expectedValueSupplier) {
    return (nodeToValidate, contextPath) -> {
      var actualValue = nodeToValidate.asText(null);
      var expectedValue = expectedValueSupplier.get();
      if (expectedValue == null) {
        throw new IllegalStateException("The supplier of the expected value for " + contextPath
          + " returned `null` and `null` is not supported for equals. Usually this indicates that the dynamic"
          + " scenario property was not properly recorded at this stage.");
      }
      if (!Objects.equals(expectedValue, actualValue)) {
        return Set.of(
          "The value of '%s' was '%s' instead of '%s'"
            .formatted(contextPath, renderValue(nodeToValidate), renderValue(expectedValue)));
      }
      return Collections.emptySet();
    };
  }

  public static JsonContentMatchedValidation matchedMustEqualIfPresent(Supplier<String> expectedValueSupplier) {
    return (nodeToValidate, contextPath) -> {
      if (isJsonNodeAbsent(nodeToValidate)) {
        return Set.of();
      }
      var actualValue = nodeToValidate.asText(null);
      var expectedValue = expectedValueSupplier.get();
      if (expectedValue == null) {
        throw new IllegalStateException("The supplier of the expected value for " + contextPath
          + " returned `null` and `null` is not supported for equals. Usually this indicates that the dynamic"
          + " scenario property was not properly recorded at this stage.");
      }
      if (!Objects.equals(expectedValue, actualValue)) {
        return Set.of(
          "The value of '%s' was '%s' instead of '%s'"
            .formatted(contextPath, renderValue(nodeToValidate), renderValue(expectedValue)));
      }
      return Collections.emptySet();
    };
  }

  public static JsonContentCheck mustBePresent(JsonPointer jsonPointer) {
    return JsonContentCheckImpl.of(jsonPointer, matchedMustBePresent());
  }

  public static JsonContentMatchedValidation matchedMustBeAbsent() {
    return (node, context) -> {
        if (!node.isMissingNode()) {
          return Set.of(
            "The attribute '%s' should have been absent but was present and had value '%s'"
              .formatted(context, renderValue(node)));
        }
        return Collections.emptySet();
      };
  }

  public static JsonContentCheck mustBeAbsent(
    JsonPointer jsonPointer
  ) {
    return JsonContentCheckImpl.of(jsonPointer, matchedMustBeAbsent());
  }

  public static JsonContentMatchedValidation combine(
    JsonContentMatchedValidation ... subchecks
  ) {
    if (subchecks.length < 2) {
      throw new IllegalArgumentException("At least two checks must be given");
    }
    return (node, context) -> {
      var r = new HashSet<String>();
      for (var check : subchecks) {
        r.addAll(check.validate(node, context));
      }
      return r;
    };
  }

  public static JsonContentMatchedValidation matchedMustBeDatasetKeywordIfPresent(
    KeywordDataset dataset
  ) {
    return (node, context) -> {
      var text = node.asText();
      // We rely on schema validation (or mustBePresent) for required check.
      if (!node.isMissingNode() && !dataset.contains(text)) {
        return Set.of(
          "The attribute '%s' had value '%s' which was not a valid keyword here."
            .formatted(context, renderValue(node)));
      }
      return Collections.emptySet();
    };
  }

  public static JsonContentCheck mustBeDatasetKeywordIfPresent(
    JsonPointer jsonPointer,
    KeywordDataset dataset
  ) {
    return JsonContentCheckImpl.of(jsonPointer, matchedMustBeDatasetKeywordIfPresent(dataset));
  }

  public static JsonContentCheck atMostOneOf(
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
    JsonContentValidation then
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
    JsonContentValidation then,
    @NonNull
    JsonContentValidation elseCheck
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

  public static JsonContentMatchedValidation ifMatchedThen(
    @NonNull
    Predicate<JsonNode> when,
    @NonNull
    JsonContentMatchedValidation then
  ) {
    return (body, context) -> {
      if (when.test(body)) {
        return then.validate(body, context);
      }
      return Set.of();
    };
  }

  public static JsonContentMatchedValidation ifMatchedThenElse(
    @NonNull
    Predicate<JsonNode> when,
    @NonNull
    JsonContentMatchedValidation then,
    @NonNull
    JsonContentMatchedValidation elseCheck
  ) {
    return (body, context) -> {
        if (when.test(body)) {
          return then.validate(body, context);
        }
        return elseCheck.validate(body, context);
      };
  }

  public static JsonContentCheck customValidator(
    @NonNull String description,
    @NonNull Function<JsonNode, Set<String>> validator
  ) {
    return JsonContentCheckImpl.of(description, validator);
  }

  public static JsonContentCheck customValidator(
    @NonNull String description,
    @NonNull JsonContentMatchedValidation validator
  ) {
    return JsonContentCheckImpl.of(description, ROOT_PTR, validator);
  }

  private static Function<JsonNode, JsonNode> at(JsonPointer jsonPointer) {
    return (refNode) -> refNode.at(jsonPointer);
  }

  private static Function<JsonNode, JsonNode> path(String path) {
    return (refNode) -> refNode.path(path);
  }

  private static Function<JsonNode, Set<String>> at(JsonPointer jsonPointer, Function<JsonNode, Set<String>> validator) {
    return at(jsonPointer).andThen(validator);
  }

  private static Function<JsonNode, Set<String>> path(String path, Function<JsonNode, Set<String>> validator) {
    return path(path).andThen(validator);
  }

  private static Function<JsonNode, Set<String>> atMatched(JsonPointer jsonPointer, JsonContentMatchedValidation validator) {
    return (refNode) -> validator.validate(refNode.at(jsonPointer), renderJsonPointer(jsonPointer));
  }

  static String renderValue(JsonNode node) {
    if (node == null || node.isMissingNode()) {
      return "(absent)";
    }
    if (node.isArray()) {
      return "[an array]";
    }
    if (node.isObject()) {
      return "{an object}";
    }
    var v = node.asText(null);
    return v == null ? "(null)" : v;
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

    private static JsonContentCheck of(String description, Function<JsonNode, Set<String>> impl) {
      return new JsonContentCheckImpl(description, impl);
    }

    private static JsonContentCheck of(JsonPointer pointer, Function<JsonNode, Set<String>> impl) {
      return new JsonContentCheckImpl(jsonCheckName(pointer), at(pointer, impl));
    }

    private static JsonContentCheck of(JsonPointer pointer, JsonContentMatchedValidation impl) {
      return of(jsonCheckName(pointer), atMatched(pointer, impl));
    }

    private static JsonContentCheck of(String description, JsonPointer pointer, JsonContentMatchedValidation impl) {
      return new JsonContentCheckImpl(description, atMatched(pointer, impl));
    }
  }
}
