package org.dcsa.conformance.core.check;

import java.util.Set;
import java.util.stream.Collectors;

public sealed interface ConformanceCheckResult {

  Set<String> getErrorMessages();

  boolean isEmpty();

  record SimpleErrors(Set<String> errors) implements ConformanceCheckResult {

    @Override
    public Set<String> getErrorMessages() {
      return errors;
    }

    @Override
    public boolean isEmpty() {
      return errors.isEmpty();
    }
  }

  record ErrorsWithRelevance(Set<ConformanceError> errors) implements ConformanceCheckResult {

    @Override
    public Set<String> getErrorMessages() {
      return errors.stream()
          .filter(
              conformanceError ->
                  !ConformanceErrorSeverity.IRRELEVANT.equals(conformanceError.severity()))
          .map(ConformanceError::message)
          .collect(Collectors.toSet());
    }

    @Override
    public boolean isEmpty() {
      return errors.isEmpty();
    }
  }

  static ConformanceCheckResult simple(Set<String> errors) {
    return new SimpleErrors(errors);
  }

  static ConformanceCheckResult withRelevance(Set<ConformanceError> errors) {
    return new ErrorsWithRelevance(errors);
  }
}
