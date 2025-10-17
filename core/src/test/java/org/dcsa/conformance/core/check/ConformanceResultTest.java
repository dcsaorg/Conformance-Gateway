package org.dcsa.conformance.core.check;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConformanceResultTest {

  @Nested
  @DisplayName("forSourceParty method tests")
  class ForSourcePartyTests {

    @Test
    void forSourceParty_withEmptyErrors_shouldBeConformant() {
      // Given
      Set<String> emptyErrors = Set.of();

      // When
      ConformanceResult result = ConformanceResult.forSourceParty(emptyErrors);

      // Then
      assertTrue(result.isRelevant());
      assertTrue(result.isConformant());
      assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void forSourceParty_withErrors_shouldNotBeConformant() {
      // Given
      Set<String> errors = Set.of("Error 1", "Error 2");

      // When
      ConformanceResult result = ConformanceResult.forSourceParty(errors);

      // Then
      assertTrue(result.isRelevant());
      assertFalse(result.isConformant());
      assertEquals(errors, result.getErrors());
    }
  }

  @Nested
  @DisplayName("forTargetParty method tests")
  class ForTargetPartyTests {

    @Test
    void forTargetParty_withEmptyErrors_shouldBeConformant() {
      // Given
      Set<String> emptyErrors = Set.of();

      // When
      ConformanceResult result = ConformanceResult.forTargetParty(emptyErrors);

      // Then
      assertTrue(result.isRelevant());
      assertTrue(result.isConformant());
      assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void forTargetParty_withErrors_shouldNotBeConformant() {
      // Given
      Set<String> errors = Set.of("Target Error 1", "Target Error 2");

      // When
      ConformanceResult result = ConformanceResult.forTargetParty(errors);

      // Then
      assertTrue(result.isRelevant());
      assertFalse(result.isConformant());
      assertEquals(errors, result.getErrors());
    }
  }

  @Nested
  @DisplayName("forSourcePartyWithRelevance method tests")
  class ForSourcePartyWithRelevanceTests {

    @Test
    void forSourcePartyWithRelevance_withNoErrors_shouldBeApplicableAndConformant() {
      // Given
      Set<ConformanceError> emptyErrors = Set.of();

      // When
      ConformanceResult result = ConformanceResult.forSourcePartyWithRelevance(emptyErrors);

      // Then
      assertTrue(result.isRelevant());
      assertTrue(result.isConformant());
      assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void forSourcePartyWithRelevance_withAllIrrelevantErrors_shouldNotBeApplicable() {
      // Given
      Set<ConformanceError> irrelevantErrors =
          Set.of(
              new ConformanceError("Irrelevant 1", ConformanceErrorSeverity.IRRELEVANT),
              new ConformanceError("Irrelevant 2", ConformanceErrorSeverity.IRRELEVANT));

      // When
      ConformanceResult result = ConformanceResult.forSourcePartyWithRelevance(irrelevantErrors);

      // Then
      assertFalse(result.isRelevant());
      assertTrue(result.isConformant()); // conformant because no real errors
      assertTrue(result.getErrors().isEmpty()); // irrelevant errors filtered out
    }

    @Test
    void forSourcePartyWithRelevance_withRealErrors_shouldBeApplicableAndNonConformant() {
      // Given
      Set<ConformanceError> realErrors =
          Set.of(
              new ConformanceError("Real Error 1", ConformanceErrorSeverity.ERROR),
              new ConformanceError("Real Error 2", ConformanceErrorSeverity.FATAL));

      // When
      ConformanceResult result = ConformanceResult.forSourcePartyWithRelevance(realErrors);

      // Then
      assertTrue(result.isRelevant());
      assertFalse(result.isConformant());
      assertEquals(Set.of("Real Error 1", "Real Error 2"), result.getErrors());
    }

    @Test
    void forSourcePartyWithRelevance_withMixedErrors_shouldFilterIrrelevantErrors() {
      // Given
      Set<ConformanceError> mixedErrors =
          Set.of(
              new ConformanceError("Real Error", ConformanceErrorSeverity.ERROR),
              new ConformanceError("Irrelevant Error", ConformanceErrorSeverity.IRRELEVANT),
              new ConformanceError("Warning", ConformanceErrorSeverity.WARNING));

      // When
      ConformanceResult result = ConformanceResult.forSourcePartyWithRelevance(mixedErrors);

      // Then
      assertTrue(result.isRelevant()); // has real errors
      assertFalse(result.isConformant()); // has real errors
      assertEquals(Set.of("Real Error", "Warning"), result.getErrors()); // only non-irrelevant
    }

    @Test
    void forSourcePartyWithRelevance_withWarningErrors_shouldTreatAsRealErrors() {
      // Given
      Set<ConformanceError> warningErrors =
          Set.of(
              new ConformanceError("Warning 1", ConformanceErrorSeverity.WARNING),
              new ConformanceError("Warning 2", ConformanceErrorSeverity.WARNING));

      // When
      ConformanceResult result = ConformanceResult.forSourcePartyWithRelevance(warningErrors);

      // Then
      assertTrue(result.isRelevant());
      assertFalse(result.isConformant());
      assertEquals(Set.of("Warning 1", "Warning 2"), result.getErrors());
    }

    @Test
    void forSourcePartyWithRelevance_withSingleIrrelevantError_shouldNotBeApplicable() {
      // Given
      Set<ConformanceError> singleIrrelevantError =
          Set.of(new ConformanceError("Only irrelevant", ConformanceErrorSeverity.IRRELEVANT));

      // When
      ConformanceResult result =
          ConformanceResult.forSourcePartyWithRelevance(singleIrrelevantError);

      // Then
      assertFalse(result.isRelevant());
      assertTrue(result.isConformant());
      assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void forSourcePartyWithRelevance_withEmptyErrorMessage_shouldIncludeEmptyMessage() {
      // Given
      Set<ConformanceError> errorsWithEmptyMessage =
          Set.of(
              new ConformanceError("", ConformanceErrorSeverity.ERROR),
              new ConformanceError("Real Error", ConformanceErrorSeverity.ERROR));

      // When
      ConformanceResult result =
          ConformanceResult.forSourcePartyWithRelevance(errorsWithEmptyMessage);

      // Then
      assertTrue(result.isRelevant());
      assertFalse(result.isConformant());
      assertEquals(Set.of("", "Real Error"), result.getErrors());
    }
  }

  @Nested
  @DisplayName("forTargetPartyWithRelevance method tests")
  class ForTargetPartyWithRelevanceTests {

    @Test
    void forTargetPartyWithRelevance_shouldDelegateToSourcePartyMethod() {
      // Given
      Set<ConformanceError> mixedErrors =
          Set.of(
              new ConformanceError("Real Error", ConformanceErrorSeverity.ERROR),
              new ConformanceError("Irrelevant Error", ConformanceErrorSeverity.IRRELEVANT));

      // When
      ConformanceResult targetResult = ConformanceResult.forTargetPartyWithRelevance(mixedErrors);
      ConformanceResult sourceResult = ConformanceResult.forSourcePartyWithRelevance(mixedErrors);

      // Then
      assertEquals(sourceResult.isRelevant(), targetResult.isRelevant());
      assertEquals(sourceResult.isConformant(), targetResult.isConformant());
      assertEquals(sourceResult.getErrors(), targetResult.getErrors());
    }
  }

  @Nested
  @DisplayName("Edge cases and error handling")
  class EdgeCasesTests {

    @Test
    void shouldPreserveErrorMessageUniqueness() {
      // Given
      Set<ConformanceError> duplicateMessages =
          Set.of(
              new ConformanceError("Duplicate Error", ConformanceErrorSeverity.ERROR),
              new ConformanceError("Duplicate Error", ConformanceErrorSeverity.WARNING),
              new ConformanceError("Unique Error", ConformanceErrorSeverity.ERROR));

      // When
      ConformanceResult result = ConformanceResult.forSourcePartyWithRelevance(duplicateMessages);

      // Then
      assertTrue(result.isRelevant());
      assertFalse(result.isConformant());
      assertEquals(Set.of("Duplicate Error", "Unique Error"), result.getErrors());
    }
  }
}
