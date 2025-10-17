package org.dcsa.conformance.core.check;

import java.util.Set;

public sealed interface ConformanceCheckResult {

  record SimpleErrors(Set<String> errors) implements ConformanceCheckResult {}

  record ErrorsWithRelevance(Set<ConformanceError> errors) implements ConformanceCheckResult {}

  static ConformanceCheckResult simple(Set<String> errors) {
    return new SimpleErrors(errors);
  }

  static ConformanceCheckResult withRelevance(Set<ConformanceError> errors) {
    return new ErrorsWithRelevance(errors);
  }
}
