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

  static ConformanceCheckResult from(ConformanceCheckResult other) {
    return switch (other) {
      case SimpleErrors(var errors) ->
          withRelevance(errors.stream().map(ConformanceError::error).collect(Collectors.toSet()));
      case ErrorsWithRelevance(var errors) -> withRelevance(errors);
    };
  }

  static ConformanceCheckResult from(Set<ConformanceCheckResult> others) {
    return withRelevance(
        others.stream()
            .map(ConformanceCheckResult::from)
            .flatMap(result -> ((ErrorsWithRelevance) result).errors().stream())
            .collect(Collectors.toSet()));
  }
}
