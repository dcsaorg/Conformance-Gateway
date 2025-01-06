package org.dcsa.conformance.standards.jit.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JitServiceTypeSelector {
  GIVEN("Given"),
  FULL_ERP("full ERP"),
  S_A_PATTERN("S-A pattern"),
  ;

  private final String fullName;
}
