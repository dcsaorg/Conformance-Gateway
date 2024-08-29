package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class JsonAttribute {

  private static final BiFunction<JsonNode, String, Set<String>> EMPTY_VALIDATOR = (ignoredA, ignoredB) -> Set.of();

  public static ActionCheck contentChecks(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    String standardsVersion,
    JsonContentCheck ... checks
  ) {
    return contentChecks("", isRelevantForRoleName, matchedExchangeUuid, httpMessageType, standardsVersion, Arrays.asList(checks));
  }

  @Deprecated
  public static ActionCheck contentChecks(
    String titlePrefix,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    String standardsVersion,
    JsonContentCheck ... checks
  ) {
    return contentChecks(titlePrefix, isRelevantForRoleName, matchedExchangeUuid, httpMessageType, standardsVersion, Arrays.asList(checks));
  }

  public static ActionCheck contentChecks(
    String titlePrefix,
    String title,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    String standardsVersion,
    JsonContentCheck ... checks
  ) {
    return contentChecks(titlePrefix, title, isRelevantForRoleName, matchedExchangeUuid, httpMessageType, standardsVersion, Arrays.asList(checks));
  }


  public static ActionCheck contentChecks(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    String standardsVersion,
    List<JsonContentCheck> checks
  ) {
    return contentChecks("", isRelevantForRoleName, matchedExchangeUuid, httpMessageType, standardsVersion, checks);
  }

  public static ActionCheck contentChecks(
    String titlePrefix,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    String standardVersion,
    List<JsonContentCheck> checks
  ) {
    return contentChecks(
      titlePrefix,
      "The HTTP %s has valid content (conditional validation rules)"
        .formatted(httpMessageType.name().toLowerCase()),
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      standardVersion,
      checks
    );
  }

  public static ActionCheck contentChecks(
    String titlePrefix,
    String title,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    String standardsVersion,
    List<JsonContentCheck> checks
  ) {
    return new JsonAttributeBasedCheck(
      titlePrefix,
      title,
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      standardsVersion,
      checks
    );
  }

  public static ActionCheck contentChecks(
    String title,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    String standardsVersion,
    JsonContentCheckRebaser rebaser,
    List<JsonRebaseableContentCheck> checks
  ) {
    return contentChecks(
      "",
      title,
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      standardsVersion,
      rebaser,
      checks
    );
  }

  public static ActionCheck contentChecks(
    String titlePrefix,
    String title,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    String standardsVersion,
    JsonContentCheckRebaser rebaser,
    List<JsonRebaseableContentCheck> checks
  ) {
    return new JsonRebaseableAttributeBasedCheck(
      titlePrefix,
      title,
      isRelevantForRoleName,
      matchedExchangeUuid,
      httpMessageType,
      standardsVersion,
      rebaser,
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
    return baseNode -> baseNode.at(jsonPointer).asBoolean(defaultValue);
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
    return baseNode -> baseNode.path(path).asBoolean(defaultValue);
  }

  public static Predicate<JsonNode> isFalse(
    @NonNull
    String path
  ) {
    return isFalse(path, true);
  }

  public static Predicate<JsonNode> isFalse(
    @NonNull
    String path,
    boolean defaultValue
  ) {
    return baseNode -> !baseNode.path(path).asBoolean(defaultValue);
  }

  public static Predicate<JsonNode> isEqualTo(
    @NonNull String path,
    @NonNull String expectedValue
  ) {
    return baseNode -> expectedValue.equals(baseNode.path(path).asText());
  }

  public static Predicate<JsonNode> isOneOf(
    @NonNull String path,
    @NonNull Set<String> expectedValue
  ) {
    return baseNode -> expectedValue.contains(baseNode.path(path).asText());
  }

  public static Predicate<JsonNode> isNotNull(
    @NonNull
    JsonPointer jsonPointer
  ) {
    return root -> {
      var node = at(jsonPointer).apply(root);
      return !node.isMissingNode() && !node.isNull();
    };
  }

  public static JsonRebaseableContentCheck lostAttributeCheck(
    @NonNull
    String description,
    @NonNull
    Supplier<JsonNode> baseNodeSupplier
  ) {
    return LostAttributeCheck.of(
      description,
      baseNodeSupplier,
      null
    );
  }


  public static JsonRebaseableContentCheck lostAttributeCheck(
    @NonNull
    String description,
    @NonNull
    Supplier<JsonNode> baseNodeSupplier,
    @NonNull
    BiConsumer<JsonNode, JsonNode> normalizer
  ) {
    return LostAttributeCheck.of(
      description,
      baseNodeSupplier,
      normalizer
    );
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

      return Set.of("The field '%s' being present makes '%s' mandatory".formatted(
        concatContextPath(contextPath, sourceFieldName),
        concatContextPath(contextPath, impliedFieldName)
      ));
    };
  }
  public static JsonRebaseableContentCheck allIndividualMatchesMustBeValid(
    @NonNull
    String name,
    @NonNull
    Consumer<MultiAttributeValidator> scanner,
    @NonNull
    JsonContentMatchedValidation subvalidation
  ) {
    return new JsonRebaseableCheckImpl(
        name,
        (body, contextPath) -> {
          var v = new MultiAttributeValidatorImpl(contextPath, body, subvalidation);
          scanner.accept(v);
          return v.getValidationIssues();
        });
  }

  public static JsonContentMatchedValidation unique(
    String field
  ) {
    return unique(
      "field %s with value".formatted(field),
      node -> node.path(field).asText(null)
    );
  }

  public static JsonContentMatchedValidation unique(
    String fieldA,
    String fieldB
  ) {
    return unique(
      "combination of %s/%s with value".formatted(fieldA, fieldB),
      node -> {
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
      var fullContext = concatContextPath(contextPath, path);
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

  public static String concatContextPath(String contextPath, String nextPathSegment) {
    return contextPath.isEmpty() ? nextPathSegment : contextPath + "." + nextPathSegment;
  }

  public static JsonContentMatchedValidation matchedMustBeNonEmpty() {
    return (node, contextPath) -> {
        if (node.isMissingNode() || node.isNull() || node.isEmpty()) {
          return Set.of(
            "The value of '%s' must present and non-empty"
              .formatted(contextPath));
        }
        return Collections.emptySet();
      };
  }

  public static JsonContentMatchedValidation matchedMustBeNotNull() {
    return (node, contextPath) -> {
      if (node.isMissingNode() || node.isNull()) {
        return Set.of(
          "The value of '%s' must present and not null"
            .formatted(contextPath));
      }
      return Set.of();
    };
  }

  public static JsonRebaseableContentCheck mustBeNotNull(
    JsonPointer jsonPointer,
    String reason
  ) {
    return new JsonRebaseableCheckImpl(
        jsonCheckName(jsonPointer),
        (body, contextPath) -> {
          var node = body.at(jsonPointer);
          if (node.isMissingNode() || node.isNull()) {
            return Set.of(
                "The value of '%s' must present and not null because %s"
                    .formatted(renderJsonPointer(jsonPointer, contextPath), reason));
          }
          return Set.of();
        });
  }


  public static JsonRebaseableContentCheck mustEqual(
      JsonPointer jsonPointer,
      String expectedValue) {
    Objects.requireNonNull(
      expectedValue,
      "expectedValue cannot be null; Note: Use `() -> getDspSupplier().get().foo()` (or similar) when testing a value against a dynamic scenario property"
    );
    return new JsonRebaseableCheckImpl(
        "%s: Must equal '%s'".formatted(jsonCheckName(jsonPointer), expectedValue),
        (body, contextPath) -> {
          var node = body.at(jsonPointer);
          var actualValue = node.asText(null);
          if (!Objects.equals(expectedValue, actualValue)) {
            return Set.of(
                "The value of '%s' was '%s' instead of '%s'"
                    .formatted(
                        renderJsonPointer(jsonPointer, contextPath),
                        renderValue(node),
                        renderValue(expectedValue)));
          }
          return Collections.emptySet();
        });
  }


  public static JsonRebaseableContentCheck mustEqual(
    String title,
    JsonPointer jsonPointer,
    boolean expectedValue) {
    return new JsonRebaseableCheckImpl(
      title,
      (body, contextPath) -> {
        var node = body.at(jsonPointer);
        if (!node.isBoolean() || node.asBoolean() != expectedValue) {
          return Set.of(
            "The value of '%s' was '%s' instead of '%s'"
              .formatted(
                renderJsonPointer(jsonPointer, contextPath),
                renderValue(node),
                expectedValue));
        }
        return Collections.emptySet();
      });
  }

  public static JsonRebaseableContentCheck mustEqual(
    JsonPointer jsonPointer,
    @NonNull
    Supplier<String> expectedValueSupplier) {
    return mustEqual(
      jsonCheckName(jsonPointer),
      jsonPointer,
      expectedValueSupplier
    );
  }


  public static JsonRebaseableContentCheck mustEqual(
    String name,
    JsonPointer jsonPointer,
    @NonNull
    Supplier<String> expectedValueSupplier) {
    var v = expectedValueSupplier.get();
    var context = "";
    if (v != null) {
      context = ": Must equal '%s'".formatted(v);
    }
    return new JsonRebaseableCheckImpl(
      name + context,
      (body, contextPath) -> {
          var node = body.at(jsonPointer);
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
                .formatted(renderJsonPointer(jsonPointer, contextPath), renderValue(node), renderValue(expectedValue)));
          }
          return Collections.emptySet();
        }
      );
  }


  public static JsonRebaseableContentCheck mustEqual(
    String name,
    String path,
    @NonNull
    Supplier<String> expectedValueSupplier) {
    var v = expectedValueSupplier.get();
    var context = "";
    if (v != null) {
      context = ": Must equal '%s'".formatted(v);
    }
    return new JsonRebaseableCheckImpl(
      name + context,
      (body, contextPath) -> {
        var node = body.path(path);
        var actualValue = node.asText(null);
        var nodePath = concatContextPath(contextPath, path);
        var expectedValue = expectedValueSupplier.get();
        if (expectedValue == null) {
          throw new IllegalStateException("The supplier of the expected value for " + nodePath
            + " returned `null` and `null` is not supported for equals. Usually this indicates that the dynamic"
            + " scenario property was not properly recorded at this stage.");
        }
        if (!Objects.equals(expectedValue, actualValue)) {
          return Set.of(
            "The value of '%s' was '%s' instead of '%s'"
              .formatted(nodePath, renderValue(node), renderValue(expectedValue)));
        }
        return Collections.emptySet();
      }
    );
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

  public static JsonRebaseableContentCheck mustBePresent(JsonPointer jsonPointer) {
    return JsonRebaseableCheckImpl.of(jsonPointer, matchedMustBePresent()::validate);
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

  public static JsonRebaseableContentCheck mustBeAbsent(
    JsonPointer jsonPointer
  ) {
    return JsonRebaseableCheckImpl.of(jsonPointer, matchedMustBeAbsent()::validate);
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

  public static JsonRebaseableContentCheck mustBeDatasetKeywordIfPresent(
    JsonPointer jsonPointer,
    KeywordDataset dataset
  ) {
    return JsonRebaseableCheckImpl.of(
      renderJsonPointer(jsonPointer),
      jsonPointer,
      matchedMustBeDatasetKeywordIfPresent(dataset)::validate
    );
  }

  public static JsonRebaseableContentCheck atMostOneOf(
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
    return new JsonRebaseableCheckImpl(
      name,
      (body, contextPath) -> {
        var present = Arrays.stream(ptrs)
          .filter(p -> isJsonNodePresent(body.at(p)))
          .toList();
        if (present.size() < 2) {
          return Set.of();
        }
        return Set.of(
          "At most one of the following can be present: %s".formatted(
            present.stream()
              .map(ptr -> JsonAttribute.renderJsonPointer(ptr, contextPath))
              .collect(Collectors.joining(", "
              ))
        ));
      });
  }

  public static JsonRebaseableContentCheck atLeastOneOf(
    @NonNull JsonPointer ... ptrs
  ) {
    if (ptrs.length < 2) {
      throw new IllegalStateException("At least two arguments are required");
    }
    String name = "The following are conditionally required (at least one of): %s".formatted(
      Arrays.stream(ptrs)
        .map(JsonAttribute::renderJsonPointer)
        .collect(Collectors.joining(", "))
    );
    return new JsonRebaseableCheckImpl(
      name,
      (body, contextPath) -> {
        var present = Arrays.stream(ptrs)
          .anyMatch(p -> isJsonNodePresent(body.at(p)));
        if (present) {
          return Set.of();
        }
        return Set.of(
          "At least one of the following must be present: %s".formatted(
            Arrays.stream(ptrs)
              .map(ptr -> JsonAttribute.renderJsonPointer(ptr, contextPath))
              .collect(Collectors.joining(", "))
          ));
      });
  }

  public static JsonContentMatchedValidation atLeastOneOfMatched(
    @NonNull BiConsumer<JsonNode, List<JsonPointer>> ptrSupplier
  ) {
    return (body, contextPath) -> {
      var ptrs = new ArrayList<JsonPointer>();
      ptrSupplier.accept(body, ptrs);
      if (ptrs.isEmpty()) {
        return Set.of();
      }
      var present = ptrs
        .stream()
        .anyMatch(p -> isJsonNodePresent(body.at(p)));
      if (present) {
        return Set.of();
      }
      return Set.of(
        "At least one of the following must be present: %s".formatted(
          ptrs.stream()
            .map(ptr -> JsonAttribute.renderJsonPointer(ptr, contextPath))
            .collect(Collectors.joining(", "))
        ));
      };
  }

  public static JsonRebaseableContentCheck allOrNoneArePresent(
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
    return new JsonRebaseableCheckImpl(
        name,
        (body, contextPath) -> {
          var firstPtr = ptrs[0];
          var firstNode = body.at(firstPtr);
          Predicate<JsonNode> check;
          if (firstNode.isMissingNode() || firstNode.isNull()) {
            check = JsonAttribute::isJsonNodePresent;
          } else {
            check = JsonAttribute::isJsonNodeAbsent;
          }
          var conflictingPtr =
              Arrays.stream(ptrs).filter(p -> check.test(body.at(p))).findAny().orElse(null);
          if (conflictingPtr != null) {
            return Set.of(
                "'%s' and '%s' must both be present or absent"
                    .formatted(
                        renderJsonPointer(firstPtr, contextPath),
                        renderJsonPointer(conflictingPtr, contextPath)));
          }
          return Set.of();
        });
  }

  public static JsonContentMatchedValidation combineAndValidateAgainstDataset(
    KeywordDataset dataset,
    String nameA,
    String nameB
  ) {
    return (nodeToValidate, contextPath) -> {
      var codeA = nodeToValidate.path(nameA).asText("");
      var codeB = nodeToValidate.path(nameB).asText("");
      var combined = codeA + "/" + codeB;
      if (!dataset.contains(combined)) {
        return Set.of(
          "The combination of '%s' ('%s') and '%s' ('%s') used in '%s' is not known to be a valid combination.".
            formatted(codeA, nameA, codeB, nameB, contextPath)
        );
      }
      return Set.of();
    };
  }

  public static JsonRebaseableContentCheck ifThen(
    @NonNull
    String name,
    @NonNull
    Predicate<JsonNode> when,
    @NonNull
    JsonRebaseableContentCheck then
  ) {
    return ifThenElse(name, when, then::validate, EMPTY_VALIDATOR);
  }

  public static JsonRebaseableContentCheck ifThenElse(
    @NonNull
    String name,
    @NonNull
    Predicate<JsonNode> when,
    @NonNull
    BiFunction<JsonNode, String, Set<String>> then,
    @NonNull
    BiFunction<JsonNode, String, Set<String>> elseCheck
  ) {
    return new JsonRebaseableCheckImpl(
        name,
        (body, contextPath) -> {
          if (when.test(body)) {
            return then.apply(body, contextPath);
          }
          return elseCheck.apply(body, contextPath);
        });
  }

  public static JsonRebaseableContentCheck ifThenElse(
    @NonNull
    String name,
    @NonNull
    Predicate<JsonNode> when,
    @NonNull
    JsonRebaseableContentCheck then,
    @NonNull
    JsonRebaseableContentCheck elseCheck
  ) {
    return ifThenElse(name, when, (BiFunction<JsonNode, String, Set<String>>) then::validate, elseCheck::validate);
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

  public static JsonRebaseableContentCheck customValidator(
    @NonNull String description,
    @NonNull JsonContentMatchedValidation validator
  ) {
    return new JsonRebaseableCheckImpl(description, validator::validate);
  }

  private static Function<JsonNode, JsonNode> at(JsonPointer jsonPointer) {
    return (refNode) -> refNode.at(jsonPointer);
  }

  private static Function<JsonNode, Set<String>> at(JsonPointer jsonPointer, Function<JsonNode, Set<String>> validator) {
    return at(jsonPointer).andThen(validator);
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

  static String renderJsonPointer(JsonPointer jsonPointer, String contextPath) {
    var pointer = jsonPointer.toString().substring(1).replace("/", ".");
    if (contextPath.isEmpty()) {
      return pointer;
    }
    return contextPath + "." + pointer;
  }

  static String renderJsonPointer(JsonPointer jsonPointer) {
    return renderJsonPointer(jsonPointer, "");
  }

  private static String jsonCheckName(JsonPointer jsonPointer) {
    return "The JSON body has a correct %s"
      .formatted(renderJsonPointer(jsonPointer));
  }

  public static boolean isJsonNodePresent(JsonNode node) {
    return !node.isMissingNode() && !node.isNull();
  }

  public static boolean isJsonNodeAbsent(JsonNode node) {
    return !isJsonNodePresent(node);
  }

  static JsonContentCheckRebaser rebaserFor(List<String> paths) {
    if (paths.isEmpty()) {
      throw new IllegalStateException("No paths");
    }
    return jsonContentMatchedValidation -> (node, contextPath) -> {
      for (var path : paths) {
        node = node.path(path);
        contextPath = concatContextPath(contextPath, path);
      }
      return jsonContentMatchedValidation.validate(node, contextPath);
    };
  }

  record JsonRebaseableCheckImpl(
    String description,
    BiFunction<JsonNode, String, Set<String>> impl
  ) implements JsonRebaseableContentCheck {
    @Override
    public Set<String> validate(JsonNode body, String contextPath) {
      return impl.apply(body, contextPath);
    }

    public static JsonRebaseableContentCheck of(JsonPointer jsonPointer, BiFunction<JsonNode, String, Set<String>> validator) {
      return of(jsonCheckName(jsonPointer), jsonPointer, validator);
    }

    public static JsonRebaseableContentCheck of(String description, JsonPointer jsonPointer, BiFunction<JsonNode, String, Set<String>> validator) {
      return new JsonRebaseableCheckImpl(
        description,
        (refNode, context) -> {
          var node = refNode.at(jsonPointer);
          var path = renderJsonPointer(jsonPointer, context);
          return validator.apply(node, path);
        }
      );
    }
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

    private static JsonContentCheck of(JsonPointer pointer, JsonContentMatchedValidation impl) {
      return of(jsonCheckName(pointer), atMatched(pointer, impl));
    }

  }
}
