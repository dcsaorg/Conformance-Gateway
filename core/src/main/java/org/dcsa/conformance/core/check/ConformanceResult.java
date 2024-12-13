package org.dcsa.conformance.core.check;

import java.util.*;
import lombok.Getter;

@Getter
public class ConformanceResult {
  private final boolean conformant;
  private final Set<String> errors;

  private ConformanceResult(
      boolean conformant,
      Set<String> errors) {
    this.conformant = conformant;
    this.errors = Collections.unmodifiableSet(errors);
  }

  public static ConformanceResult forSourceParty(Set<String> errors) {
    return new ConformanceResult(errors.isEmpty(), errors);
  }

  public static ConformanceResult forTargetParty(Set<String> errors) {
    return new ConformanceResult(errors.isEmpty(), errors);
  }
}
