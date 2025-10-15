package org.dcsa.conformance.core.check;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class JsonAttributeTest {

  private ObjectNode objectNode;
  private ArrayNode arrayNode;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final JsonContentCheck jsonContentCheck =
      new JsonContentCheck() {
        @Override
        public String description() {
          return "test";
        }

        @Override
        public boolean isApplicable() {
          return true;
        }

        @Override
        public Set<String> validate(JsonNode body) {
          return Collections.emptySet();
        }
      };

  @BeforeEach
  void setUp() {
    // Initialize in @BeforeEach
    arrayNode = JsonNodeFactory.instance.arrayNode();
    objectNode = JsonNodeFactory.instance.objectNode();
  }

  @Test
  void testContentChecksWithVarArgs() {
    Predicate<String> predicate = s -> true;
    assertDoesNotThrow(
        () ->
            JsonAttribute.contentChecks(
                predicate, UUID.randomUUID(), HttpMessageType.REQUEST, "2.0.0", jsonContentCheck));
  }

  @Test
  void testContentChecksWithList() {
    Predicate<String> predicate = s -> true;
    assertDoesNotThrow(
        () ->
            JsonAttribute.contentChecks(
                predicate,
                UUID.randomUUID(),
                HttpMessageType.REQUEST,
                "2.0.0",
                List.of(jsonContentCheck)));
  }

  @Test
  void testIsTrueWithJsonPointer() {
    objectNode.put("test", true);
    assertTrue(JsonAttribute.isTrue(JsonPointer.compile("/test")).test(objectNode));
    assertFalse(JsonAttribute.isTrue(JsonPointer.compile("/testFalse")).test(objectNode));
  }

  @Test
  void testIsTrueWithStringPath() {
    objectNode.put("test", true);
    assertTrue(JsonAttribute.isTrue("test").test(objectNode));
    assertFalse(JsonAttribute.isTrue("testFalse").test(objectNode));
  }

  @Test
  void testIsFalseWithStringPath() {
    objectNode.put("test", false);
    assertTrue(JsonAttribute.isFalse("test").test(objectNode));
    assertFalse(JsonAttribute.isFalse("testFalse").test(objectNode));
  }

  @Test
  void testIsEqualTo() {
    objectNode.put("test", "test");
    assertTrue(JsonAttribute.isEqualTo("test", "test").test(objectNode));
    assertFalse(JsonAttribute.isEqualTo("test", "testFalse").test(objectNode));
  }

  @Test
  void testIsOneOf() {
    objectNode.put("test", "test");

    assertTrue(JsonAttribute.isOneOf("test", Set.of("test", "test1")).test(objectNode));
    assertFalse(JsonAttribute.isOneOf("test", Set.of("test2", "test1")).test(objectNode));
  }

  @Test
  void testMustBePresent() {
    objectNode.put("test", "value");

    assertTrue(
        JsonAttribute.mustBePresent(JsonPointer.compile("/test"))
            .validate(objectNode, "")
            .isEmpty());
    assertFalse(
        JsonAttribute.mustBePresent(JsonPointer.compile("/nonexistent"))
            .validate(objectNode, "")
            .isEmpty());
  }

  @Test
  void testIfThen() {
    objectNode.put("test", "test");
    objectNode.put("test2", "test2");
    assertTrue(
        JsonAttribute.ifThen(
                "Test ifThen with correct value",
                JsonAttribute.isNotNull(JsonPointer.compile("/test")),
                JsonAttribute.mustBePresent(JsonPointer.compile("/test2")))
            .validate(objectNode, "")
            .isEmpty());

    assertFalse(
        JsonAttribute.ifThen(
                "Test ifThen with incorrect value",
                JsonAttribute.isNotNull(JsonPointer.compile("/test")),
                JsonAttribute.mustBePresent(JsonPointer.compile("/test3")))
            .validate(objectNode, "")
            .isEmpty());

    // Test when initial condition is false
    assertTrue(
        JsonAttribute.ifThen(
                "Test ifThen with initial condition false",
                JsonAttribute.isNotNull(JsonPointer.compile("/test3")),
                JsonAttribute.mustBePresent(JsonPointer.compile("/test2")))
            .validate(objectNode, "")
            .isEmpty());
  }

  static Stream<Arguments> testIsNotNullArgs() {
    return Stream.of(
        Arguments.of("test isNotNull", JsonNodeFactory.instance.objectNode(), "/test", false),
        Arguments.of(
            "test isNotNull with existing value",
            JsonNodeFactory.instance.objectNode().put("test", "test"),
            "/test",
            true));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("testIsNotNullArgs")
  void testIsNotNull(String testName, ObjectNode input, String path, boolean expected) {
    assertEquals(expected, JsonAttribute.isNotNull(JsonPointer.compile(path)).test(input));
  }

  @Test
  void testMustBeNotNull() {
    objectNode.put("test", "test");
    String reason = "test reason";

    assertTrue(
        JsonAttribute.mustBeNotNull(JsonPointer.compile("/test"), reason)
            .validate(objectNode)
            .isEmpty());
    assertFalse(
        JsonAttribute.mustBeNotNull(JsonPointer.compile("/test1"), reason)
            .validate(objectNode)
            .isEmpty());
  }

  @Test
  void testMustEqual() {
    objectNode.put("test", "test");

    assertTrue(
        JsonAttribute.mustEqual(JsonPointer.compile("/test"), "test")
            .validate(objectNode)
            .isEmpty());
    assertFalse(
        JsonAttribute.mustEqual(JsonPointer.compile("/test"), "test1")
            .validate(objectNode)
            .isEmpty());
    Supplier<String> supplier = () -> "test";

    assertTrue(
        JsonAttribute.mustEqual(JsonPointer.compile("/test"), supplier)
            .validate(objectNode)
            .isEmpty());
    supplier = () -> "test1";
    assertFalse(
        JsonAttribute.mustEqual(JsonPointer.compile("/test"), supplier)
            .validate(objectNode)
            .isEmpty());
  }

  @Test
  void testPath() {
    objectNode.put("test", "test");

    assertTrue(
        JsonAttribute.path("test", JsonAttribute.matchedMustBePresent())
            .validate(objectNode, "")
            .isEmpty());
    assertFalse(
        JsonAttribute.path("testFalse", JsonAttribute.matchedMustBePresent())
            .validate(objectNode, "")
            .isEmpty());
  }

  @Test
  void testLostAttributeCheck() {
    objectNode.put("test", "test");
    Supplier<JsonNode> supplier = () -> objectNode;

    assertDoesNotThrow(() -> JsonAttribute.lostAttributeCheck("test", supplier));

    BiConsumer<JsonNode, JsonNode> normalizer = (a, b) -> {};
    assertDoesNotThrow(() -> JsonAttribute.lostAttributeCheck("test", supplier, normalizer));
  }

  @Test
  void testPresenceImpliesOtherField() {
    objectNode.put("test", "test");
    objectNode.put("test1", "test1");

    assertTrue(
        JsonAttribute.presenceImpliesOtherField("test", "test1")
            .validate(objectNode, "")
            .isEmpty());
    assertFalse(
        JsonAttribute.presenceImpliesOtherField("test", "test3")
            .validate(objectNode, "")
            .isEmpty());
  }

  @Test
  void testAllIndividualMatchesMustBeValid() {
    objectNode.put("test", "test");
    arrayNode.add(objectNode);

    String name = "test";
    Consumer<MultiAttributeValidator> consumer =
        multiAttributeValidator -> multiAttributeValidator.submitAllMatching("test.*");
    JsonContentMatchedValidation subvalidation = (a, b) -> Set.of();

    assertTrue(
        JsonAttribute.allIndividualMatchesMustBeValid(name, consumer, subvalidation)
            .validate(arrayNode, "")
            .isEmpty());
  }

  @Test
  void testUnique() {
    objectNode.put("test", "test");
    arrayNode.add(objectNode);
    arrayNode.add(objectNode);

    Function<JsonNode, String> keyFunction = jsonNode -> jsonNode.path("test").asText(null);
    assertFalse(JsonAttribute.unique("test", keyFunction).validate(arrayNode, "").isEmpty());
  }

  @Test
  void testPathValidation() {
    objectNode.put("test", "test");

    assertTrue(
        JsonAttribute.path("test", JsonAttribute.matchedMustEqual(() -> "test"))
            .validate(objectNode, "")
            .isEmpty());
  }

  @Test
  void testAtValidation() {
    objectNode.put("test", "test");

    assertTrue(
        JsonAttribute.at(JsonPointer.compile("/test"), JsonAttribute.matchedMustEqual(() -> "test"))
            .validate(objectNode, "")
            .isEmpty());
  }

  @Test
  void testConcatContextPath() {
    assertEquals("a.b", JsonAttribute.concatContextPath("a", "b"));
  }

  @Test
  void testMatchedMustBeNonEmpty() {
    objectNode.put("test", "test");
    arrayNode.add(objectNode);

    assertTrue(JsonAttribute.matchedMustBeNonEmpty().validate(objectNode, "").isEmpty());
    assertTrue(JsonAttribute.matchedMustBeNonEmpty().validate(arrayNode, "").isEmpty());

    objectNode.removeAll();
    assertFalse(JsonAttribute.matchedMustBeNonEmpty().validate(objectNode, "").isEmpty());

    arrayNode.removeAll();
    assertFalse(JsonAttribute.matchedMustBeNonEmpty().validate(arrayNode, "").isEmpty());
  }

  @Test
  void testMatchedMustBeNotNull() {
    objectNode.put("test", "test");
    objectNode.putNull("test1");

    assertTrue(JsonAttribute.matchedMustBeNotNull().validate(objectNode, "").isEmpty());
    assertFalse(
        JsonAttribute.matchedMustBeNotNull().validate(objectNode.get("test1"), "").isEmpty());
  }

  @Test
  void testMatchedMustBeTrue() {
    objectNode.put("test", true);
    objectNode.put("testFalse", false);

    assertTrue(JsonAttribute.matchedMustBeTrue().validate(objectNode.get("test"), "").isEmpty());
    assertFalse(
        JsonAttribute.matchedMustBeTrue().validate(objectNode.get("testFalse"), "").isEmpty());
  }

  @Test
  void testMatchedMaximum() {
    objectNode.put("test", 1);

    assertTrue(JsonAttribute.matchedMaximum(2).validate(objectNode.get("test"), "").isEmpty());
    assertFalse(JsonAttribute.matchedMaximum(0).validate(objectNode.get("test"), "").isEmpty());
  }

  @Test
  void testMatchedMustBeAbsent() {
    JsonNode emptyNode = objectMapper.createObjectNode();
    ObjectNode nonEmptyNode = objectMapper.createObjectNode().put("test", "test");

    assertTrue(JsonAttribute.matchedMustBeAbsent().validate(emptyNode.path("test1"), "").isEmpty());
    assertFalse(
        JsonAttribute.matchedMustBeAbsent().validate(nonEmptyNode.get("test"), "").isEmpty());
  }

  @Test
  void testMatchedMaxLength() {
    objectNode.put("test", "test");
    arrayNode.add(objectNode);

    assertTrue(JsonAttribute.matchedMaxLength(1).validate(arrayNode, "").isEmpty());
    arrayNode.add(objectNode);
    assertFalse(JsonAttribute.matchedMaxLength(1).validate(arrayNode, "").isEmpty());
  }

  @Test
  void testCombine() {
    assertDoesNotThrow(
        () ->
            JsonAttribute.combine(
                JsonAttribute.matchedMustBeAbsent(), JsonAttribute.matchedMustBeAbsent()));
  }

  @Test
  void testMustBeDatasetKeyWordIfPresent() {
    KeywordDataset dataset = Mockito.mock(KeywordDataset.class);
    Mockito.when(dataset.contains(any())).thenReturn(true);

    objectNode.put("test", "test");

    assertTrue(
        JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/test"), dataset)
            .validate(objectNode, "")
            .isEmpty());
  }

  @Test
  void testAtMostOneOf() {
    objectNode.put("test", "test");

    assertTrue(
        JsonAttribute.atMostOneOf(JsonPointer.compile("/test"), JsonPointer.compile("/test1"))
            .validate(objectNode, "")
            .isEmpty());
    objectNode.put("test1", "test1");
    assertFalse(
        JsonAttribute.atMostOneOf(JsonPointer.compile("/test"), JsonPointer.compile("/test1"))
            .validate(objectNode, "")
            .isEmpty());
  }

  @Test
  void testAtLeastOneOf() {
    objectNode.put("test", "test");

    assertTrue(
        JsonAttribute.atLeastOneOf(JsonPointer.compile("/test"), JsonPointer.compile("/test1"))
            .validate(objectNode, "")
            .isEmpty());
    objectNode.removeAll();
    assertFalse(
        JsonAttribute.atLeastOneOf(JsonPointer.compile("/test"), JsonPointer.compile("/test1"))
            .validate(objectNode, "")
            .isEmpty());
  }

  @Test
  void testIfThenElse() {
    objectNode.put("test", true);
    objectNode.put("test2", "value");

    JsonRebaseableContentCheck check =
        JsonAttribute.ifThenElse(
            "Test ifThenElse",
            JsonAttribute.isTrue(JsonPointer.compile("/test")),
            (body, contextPath) -> Set.of("then applied"),
            (body, contextPath) -> Set.of("else applied"));

    // Test when condition is true
    Set<String> result = check.validate(objectNode, "");
    assertTrue(result.contains("then applied"));
    assertFalse(result.contains("else applied"));

    // Test when condition is false
    objectNode.put("test", false);
    result = check.validate(objectNode, "");
    assertFalse(result.contains("then applied"));
    assertTrue(result.contains("else applied"));
  }

  @Test
  void testIfMatchedThen() {
    objectNode.put("test", true);
    objectNode.put("test2", "value");

    JsonContentMatchedValidation check =
        JsonAttribute.ifMatchedThen(
            JsonAttribute.isTrue(JsonPointer.compile("/test")),
            JsonAttribute.matchedMustBePresent());

    // Test when condition is true
    Set<String> result = check.validate(objectNode, "");
    assertTrue(result.isEmpty());

    // Test when condition is false
    objectNode.put("test", false);
    result = check.validate(objectNode, "");
    assertTrue(result.isEmpty());
  }

  @Test
  void testIfMatchedThenElse() {
    objectNode.put("test", true);
    objectNode.put("test2", "value");

    JsonContentMatchedValidation check =
        JsonAttribute.ifMatchedThenElse(
            JsonAttribute.isTrue(JsonPointer.compile("/test")),
            JsonAttribute.matchedMustBePresent(),
            JsonAttribute.matchedMustBeAbsent());

    // Test when condition is true
    Set<String> result = check.validate(objectNode, "");
    assertTrue(result.isEmpty());

    // Test when condition is false
    objectNode.put("test", false);
    result = check.validate(objectNode, "");
    assertFalse(result.isEmpty());

    // Test when condition is true but validation fails
    objectNode.put("test", true);
    objectNode.remove("test2");
    objectNode.remove("test");
    result = check.validate(objectNode, "");
    assertFalse(result.isEmpty());
  }

  @Test
  void testMatchedMustBePresent() {
    objectNode.put("test", "value");

    // Test when the node is present
    assertTrue(
        JsonAttribute.matchedMustBePresent().validate(objectNode.path("test"), "").isEmpty());

    // Test when the node is missing
    assertFalse(
        JsonAttribute.matchedMustBePresent().validate(objectNode.path("missing"), "").isEmpty());
  }

  @Test
  void testMatchedMustBeDatasetKeywordIfPresent() {
    KeywordDataset dataset = Mockito.mock(KeywordDataset.class);
    Mockito.when(dataset.contains("validKeyword")).thenReturn(true);
    Mockito.when(dataset.contains("invalidKeyword")).thenReturn(false);

    objectNode.put("test", "validKeyword");

    JsonContentMatchedValidation validation =
        JsonAttribute.matchedMustBeDatasetKeywordIfPresent(dataset);

    // Test when the keyword is valid
    Set<String> result = validation.validate(objectNode.path("test"), "test");
    assertTrue(result.isEmpty());

    // Test when the keyword is invalid
    objectNode.put("test", "invalidKeyword");
    result = validation.validate(objectNode.path("test"), "test");
    assertFalse(result.isEmpty());
    assertTrue(
        result.contains(
            "The attribute 'test' has the value 'invalidKeyword', which is unknown and must match one of the values in the approved dataset."));
  }

  @Test
  void testMustBeAbsent() {
    objectNode.put("test", "value");

    // Test when the node is present
    assertFalse(
        JsonAttribute.mustBeAbsent(JsonPointer.compile("/test"))
            .validate(objectNode, "")
            .isEmpty());

    // Test when the node is absent
    assertTrue(
        JsonAttribute.mustBeAbsent(JsonPointer.compile("/missing"))
            .validate(objectNode, "")
            .isEmpty());
  }

  @Test
  void testMatchedMustEqual() {
    objectNode.put("test", "expectedValue");

    Supplier<String> expectedValueSupplier = () -> "expectedValue";
    JsonContentMatchedValidation validation = JsonAttribute.matchedMustEqual(expectedValueSupplier);

    // Test when the value matches
    Set<String> result = validation.validate(objectNode.path("test"), "test");
    assertTrue(result.isEmpty());

    // Test when the value does not match
    objectNode.put("test", "unexpectedValue");
    result = validation.validate(objectNode.path("test"), "test");
    assertFalse(result.isEmpty());
    assertTrue(
        result.contains("The value of 'test' was 'unexpectedValue' instead of 'expectedValue'"));

    // Test when the expected value supplier returns null
    expectedValueSupplier = () -> null;
    validation = JsonAttribute.matchedMustEqual(expectedValueSupplier);
    JsonContentMatchedValidation finalValidation = validation;
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> finalValidation.validate(objectNode.path("test"), "test"));
    assertTrue(
        exception
            .getMessage()
            .contains(
                "The supplier of the expected value for test returned `null` and `null` is not supported for equals."));
  }

  @Test
  void testIsJsonNodePresent() {
    objectNode.put("test", "value");

    // Test when the node is present and not null
    assertTrue(JsonAttribute.isJsonNodePresent(objectNode.get("test")));

    // Test when the node is explicitly set to null
    objectNode.putNull("nullNode");
    assertFalse(JsonAttribute.isJsonNodePresent(objectNode.get("nullNode")));
  }

  @Test
  void testIsJsonNodeAbsent() {
    objectNode.put("test", "value");

    // Test when the node is present and not null
    assertFalse(JsonAttribute.isJsonNodeAbsent(objectNode.get("test")));

    // Test when the node is explicitly set to null
    objectNode.putNull("nullNode");
    assertTrue(JsonAttribute.isJsonNodeAbsent(objectNode.get("nullNode")));
  }

  @Test
  void testAtLeastOneOfMatched() {
    objectNode.put("test1", "value1");
    objectNode.put("test2", "value2");

    BiConsumer<JsonNode, List<JsonPointer>> ptrSupplier =
        (node, ptrs) -> {
          ptrs.add(JsonPointer.compile("/test1"));
          ptrs.add(JsonPointer.compile("/test2"));
        };

    JsonContentMatchedValidation validation = JsonAttribute.atLeastOneOfMatched(ptrSupplier);

    // Test when at least one field is present
    Set<String> result = validation.validate(objectNode, "");
    assertTrue(result.isEmpty());

    // Test when no fields are present
    objectNode.removeAll();
    result = validation.validate(objectNode, "");
    assertFalse(result.isEmpty());
    assertTrue(result.contains("At least one of the following must be present: test1, test2"));
  }

  @Test
  void testAllOrNoneArePresent() {
    objectNode.put("test1", "value1");
    objectNode.put("test2", "value2");

    JsonRebaseableContentCheck check =
        JsonAttribute.allOrNoneArePresent(
            JsonPointer.compile("/test1"), JsonPointer.compile("/test2"));

    // Test when both fields are present
    Set<String> result = check.validate(objectNode, "");
    assertTrue(result.isEmpty());

    // Test when one field is missing
    objectNode.remove("test2");
    result = check.validate(objectNode, "");
    assertFalse(result.isEmpty());
    assertTrue(result.contains("'test1' and 'test2' must both be present or absent"));

    // Test when both fields are missing
    objectNode.remove("test1");
    result = check.validate(objectNode, "");
    assertTrue(result.isEmpty());
  }

  @Test
  void testXOrFields() {
    objectNode.put("field1", "value1");

    JsonRebaseableContentCheck check =
        JsonAttribute.xOrFields(JsonPointer.compile("/field1"), JsonPointer.compile("/field2"));

    // Test when only one field is present
    Set<String> result = check.validate(objectNode, "");
    assertTrue(result.isEmpty());

    // Test when both fields are present
    objectNode.put("field2", "value2");
    result = check.validate(objectNode, "");
    assertFalse(result.isEmpty());
    assertTrue(result.contains("Either one of them can be present, but not both : field1, field2"));

    // Test when none of the fields are present
    objectNode.removeAll();
    result = check.validate(objectNode, "");
    assertTrue(result.isEmpty());
  }

  @Test
  void testCustomValidator() {
    JsonNode validNode = JsonNodeFactory.instance.objectNode().put("test", "value");
    JsonNode invalidNode = JsonNodeFactory.instance.objectNode().put("test", "invalid");

    Function<JsonNode, Set<String>> validator =
        node -> {
          if ("value".equals(node.path("test").asText())) {
            return Collections.emptySet();
          }
          return Set.of("Invalid value for 'test'");
        };

    JsonContentCheck customCheck =
        JsonAttribute.customValidator("Custom Validator Test", validator);

    // Test with valid node
    assertTrue(customCheck.validate(validNode).isEmpty());

    // Test with invalid node
    Set<String> result = customCheck.validate(invalidNode);
    assertFalse(result.isEmpty());
    assertTrue(result.contains("Invalid value for 'test'"));
  }
}
