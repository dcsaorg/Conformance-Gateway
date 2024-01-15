package org.dcsa.conformance.standards.ebl.checks;

public enum ScenarioType {
  REGULAR_SWB,
  REGULAR_BOL,
  REEFER,
  DG,
  REGULAR_2C_2U_1E,
  REGULAR_2C_2U_2E,
  ;

  public String transportDocumentTypeCode() {
    return this == REGULAR_BOL ? "BOL" : "SWB";
  }

  public boolean isToOrder() {
    return false;
  }

  public String shipperTemplate(String version) {
    String suffix = switch (this) {
      case REGULAR_2C_2U_1E -> "2c2u1e";
      case REGULAR_2C_2U_2E -> "2c2u2e";
      default -> "request";
    };
    return "ebl-api-" + version.toLowerCase() + "-" + suffix + ".json";
  }
}
