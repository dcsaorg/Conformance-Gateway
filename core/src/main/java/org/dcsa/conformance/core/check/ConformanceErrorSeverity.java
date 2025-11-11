package org.dcsa.conformance.core.check;

import lombok.Getter;

@Getter
public enum ConformanceErrorSeverity {
  IRRELEVANT(true),
  WARNING(true),
  ERROR(false),
  FATAL(false),
  ;

  private final boolean conformant;

  ConformanceErrorSeverity(boolean conformant) {
    this.conformant = conformant;
  }
}
