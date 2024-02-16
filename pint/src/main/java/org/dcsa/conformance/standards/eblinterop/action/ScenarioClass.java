package org.dcsa.conformance.standards.eblinterop.action;

public enum ScenarioClass {

  NO_ISSUES,
  INVALID_RECIPIENT,
  FAIL_W_503,
  ;

  public boolean canResetToClass() {
    return this != INVALID_RECIPIENT;
  }
}
