package org.dcsa.conformance.core.check;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiAttributeValidatorImplTest {

  private MultiAttributeValidatorImpl validator;

  @BeforeEach
  void setUp() {
    JsonNode testNode = JsonNodeFactory.instance.objectNode();
    String contextPath = "test.context";
    JsonContentMatchedValidation simpleValidation =
        (node, path) -> ConformanceCheckResult.simple(Set.of());
    validator = new MultiAttributeValidatorImpl(contextPath, testNode, simpleValidation);
  }

  @Test
  void testGetValidationIssues_withRelevantResultsAndAllConformant_returnsSuccess()
      throws IllegalAccessException {
    // Given: Relevant and conformant results
    var relevantResult = ConformanceCheckResult.simple(Set.of()); // Relevant and conformant
    var anotherRelevantResult =
        ConformanceCheckResult.withRelevance(Set.of()); // Relevant and conformant

    addToValidationIssues(relevantResult, anotherRelevantResult);

    // When
    Set<ConformanceCheckResult> result = validator.getValidationIssues();

    // Then: Should return success
    assertEquals(1, result.size());
    assertTrue(result.iterator().next().getErrorMessages().isEmpty());
  }

  @Test
  void testGetValidationIssues_withNoRelevantResults_returnsOriginalIssues()
      throws IllegalAccessException {
    // Given: Only irrelevant results
    var irrelevantResult1 =
        ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    var irrelevantResult2 =
        ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));

    addToValidationIssues(irrelevantResult1, irrelevantResult2);

    // When
    Set<ConformanceCheckResult> result = validator.getValidationIssues();

    // Then: Should return original issues
    assertEquals(1, result.size());
    assertTrue(result.contains(irrelevantResult1));
    assertTrue(result.contains(irrelevantResult2));
  }

  @Test
  void testGetValidationIssues_withNonConformantResults_returnsOriginalIssues()
      throws IllegalAccessException {
    // Given: Results with errors (non-conformant)
    var errorResult = ConformanceCheckResult.simple(Set.of("Validation error"));
    var conformantResult = ConformanceCheckResult.simple(Set.of());
    var irrelevantResult =
        ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));

    addToValidationIssues(errorResult, conformantResult, irrelevantResult);

    // When
    Set<ConformanceCheckResult> result = validator.getValidationIssues();

    // Then: Should return original issues (including errors)
    assertEquals(3, result.size());
    assertTrue(result.contains(errorResult));
    assertTrue(result.contains(conformantResult));
  }

  @Test
  void testGetValidationIssues_withMixedIrrelevantAndConformant_returnsSuccess()
      throws IllegalAccessException {
    // Given: Mix of irrelevant and conformant results
    var irrelevantResult =
        ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    var conformantResult = ConformanceCheckResult.simple(Set.of()); // Relevant and conformant

    addToValidationIssues(irrelevantResult, conformantResult);

    // When
    Set<ConformanceCheckResult> result = validator.getValidationIssues();

    // Then: Should return success since we have relevant results and all relevant ones are
    // conformant
    assertEquals(1, result.size());
    assertTrue(result.iterator().next().getErrorMessages().isEmpty());
  }

  private void addToValidationIssues(ConformanceCheckResult... results)
      throws IllegalAccessException {
    var validationIssuesField = getValidationIssuesField();
    assertNotNull(validationIssuesField);
    @SuppressWarnings("unchecked")
    Set<ConformanceCheckResult> issues =
        (Set<ConformanceCheckResult>) validationIssuesField.get(validator);
    Collections.addAll(issues, results);
  }

  private java.lang.reflect.Field getValidationIssuesField() {
    try {
      java.lang.reflect.Field field =
          MultiAttributeValidatorImpl.class.getDeclaredField("validationIssues");
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      fail("Could not access validationIssues field: " + e.getMessage());
      return null;
    }
  }
}
