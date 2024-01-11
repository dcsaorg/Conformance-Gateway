package org.dcsa.conformance.standards.ebl.checks;

public enum ScenarioType {
  REGULAR_SWB,
  REGULAR_BOL,
  REEFER,
  DG,
  ;

  public String transportDocumentTypeCode() {
    return this == REGULAR_BOL ? "BOL" : "SWB";
  }

  public boolean isToOrder() {
    return false;
  }
}
