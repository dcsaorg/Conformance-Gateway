package org.dcsa.conformance.standards.eblissuance.action;

public enum EblType {
  STRAIGHT_EBL,
  NEGOTIABLE_EBL,
  BLANK_EBL,
  ;

  public boolean isToOrder() {
    return this != STRAIGHT_EBL;
  }

  public boolean isBlankEbl() {
    return this == BLANK_EBL;
  }
}
