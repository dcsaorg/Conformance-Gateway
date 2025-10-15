package org.dcsa.conformance.core.check;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConformanceCheckTest {

  private TestableConformanceCheck conformanceCheck;

  @BeforeEach
  void setUp() {
    conformanceCheck = new TestableConformanceCheck("Test Check");
  }

  @Test
  void isApplicable_withNoResults_shouldReturnTrue() {
    // Given: no results and default isApplicable = true

    // When & Then
    assertTrue(conformanceCheck.isApplicable());
  }

  @Test
  void isApplicable_withInstanceFieldFalse_shouldReturnFalse() {
    // Given
    conformanceCheck.setApplicable(false);

    // When & Then
    assertFalse(conformanceCheck.isApplicable());
  }

  @Test
  void isApplicable_withAllResultsNotApplicable_shouldReturnFalse() {
    // Given
    ConformanceResult notApplicableResult1 = ConformanceResult.forSourcePartyWithRelevance(
        Set.of(new ConformanceError("irrelevant error 1", ConformanceErrorSeverity.IRRELEVANT))
    );
    ConformanceResult notApplicableResult2 = ConformanceResult.forSourcePartyWithRelevance(
        Set.of(new ConformanceError("irrelevant error 2", ConformanceErrorSeverity.IRRELEVANT))
    );

    conformanceCheck.addResult(notApplicableResult1);
    conformanceCheck.addResult(notApplicableResult2);

    // When & Then
    assertFalse(conformanceCheck.isApplicable());
  }

  @Test
  void isApplicable_withMixedResults_shouldReturnInstanceFieldValue() {
    // Given
    ConformanceResult notApplicableResult = ConformanceResult.forSourcePartyWithRelevance(
        Set.of(new ConformanceError("irrelevant error", ConformanceErrorSeverity.IRRELEVANT))
    );
    ConformanceResult applicableResult = ConformanceResult.forSourcePartyWithRelevance(
        Set.of(new ConformanceError("real error", ConformanceErrorSeverity.ERROR))
    );

    conformanceCheck.addResult(notApplicableResult);
    conformanceCheck.addResult(applicableResult);

    // When & Then
    assertTrue(conformanceCheck.isApplicable()); // instance field is true by default
  }

  @Test
  void isApplicable_withInstanceFieldFalseAndApplicableResults_shouldReturnFalse() {
    // Given
    conformanceCheck.setApplicable(false);
    ConformanceResult applicableResult = ConformanceResult.forSourcePartyWithRelevance(
        Set.of(new ConformanceError("real error", ConformanceErrorSeverity.ERROR))
    );

    conformanceCheck.addResult(applicableResult);

    // When & Then
    assertFalse(conformanceCheck.isApplicable());
  }

  @Test
  void isApplicable_withOnlyApplicableResults_shouldReturnInstanceFieldValue() {
    // Given
    ConformanceResult applicableResult1 = ConformanceResult.forSourcePartyWithRelevance(
        Set.of(new ConformanceError("error 1", ConformanceErrorSeverity.ERROR))
    );
    ConformanceResult applicableResult2 = ConformanceResult.forSourcePartyWithRelevance(
        Set.of(new ConformanceError("error 2", ConformanceErrorSeverity.WARNING))
    );

    conformanceCheck.addResult(applicableResult1);
    conformanceCheck.addResult(applicableResult2);

    // When & Then
    assertTrue(conformanceCheck.isApplicable()); // instance field is true by default
  }

  @Test
  void isApplicable_withEmptyErrorSets_shouldReturnInstanceFieldValue() {
    // Given
    ConformanceResult emptyResult = ConformanceResult.forSourcePartyWithRelevance(Set.of());

    conformanceCheck.addResult(emptyResult);

    // When & Then
    assertTrue(conformanceCheck.isApplicable()); // instance field is true by default
  }

  /**
   * Testable implementation of ConformanceCheck that exposes protected methods for testing
   */
  private static class TestableConformanceCheck extends ConformanceCheck {
    public TestableConformanceCheck(String title) {
      super(title);
    }

    @Override
    public void addResult(ConformanceResult result) {
      super.addResult(result);
    }
  }
}
