package org.dcsa.conformance.core.check;

import java.util.*;
import java.util.stream.Collectors;

import lombok.Getter;

@Getter
public class ConformanceResult {

  private final boolean isApplicable;
  private final boolean conformant;
  private final Set<String> errors;

  private ConformanceResult(boolean conformant, Set<String> errors) {
    this.conformant = conformant;
    this.errors = Collections.unmodifiableSet(errors);
    this.isApplicable = true;
  }

  private ConformanceResult(boolean isApplicable, boolean conformant, Set<String> errors) {
    this.conformant = conformant;
    this.errors = Collections.unmodifiableSet(errors);
    this.isApplicable = isApplicable;
  }

  public static ConformanceResult forSourceParty(Set<String> errors) {
    return new ConformanceResult(errors.isEmpty(), errors);
  }

  public static ConformanceResult forTargetParty(Set<String> errors) {
    return new ConformanceResult(errors.isEmpty(), errors);
  }

  public static ConformanceResult forSourcePartyWithRelevance(Set<ConformanceError> errors) {
    boolean isApplicable =
        errors.isEmpty()
            || errors.stream()
                .anyMatch(
                    conformanceError ->
                        !ConformanceErrorSeverity.IRRELEVANT.equals(conformanceError.severity()));

    boolean conformant =
        errors.stream()
            .filter(
                conformanceError ->
                    !ConformanceErrorSeverity.IRRELEVANT.equals(conformanceError.severity()))
            .toList()
            .isEmpty();

    Set<String> errorMessages =
        errors.stream()
            .filter(
                conformanceError ->
                    !ConformanceErrorSeverity.IRRELEVANT.equals(conformanceError.severity()))
            .map(ConformanceError::message)
            .collect(Collectors.toSet());

    return new ConformanceResult(isApplicable, conformant, errorMessages);
  }

  public static ConformanceResult forTargetPartyWithRelevance(Set<ConformanceError> errors) {
    return forSourcePartyWithRelevance(errors);
  }
}
