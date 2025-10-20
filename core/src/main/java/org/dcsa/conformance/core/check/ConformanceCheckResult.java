package org.dcsa.conformance.core.check;

import java.util.Set;
import java.util.stream.Collectors;

public sealed interface ConformanceCheckResult {

  Set<String> getErrorMessages();

  record SimpleErrors(Set<String> errors) implements ConformanceCheckResult {

    @Override
    public Set<String> getErrorMessages() {
      return errors;
    }
  }

  record ErrorsWithRelevance(Set<ConformanceError> errors) implements ConformanceCheckResult {

    @Override
    public Set<String> getErrorMessages() {
      return errors.stream().map(ConformanceError::message).collect(Collectors.toSet());
    }
  }

  static ConformanceCheckResult simple(Set<String> errors) {
    return new SimpleErrors(errors);
  }

  static ConformanceCheckResult withRelevance(Set<ConformanceError> errors) {
    return new ErrorsWithRelevance(errors);
  }
}
