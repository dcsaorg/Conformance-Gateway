package org.dcsa.conformance.standards.ebl.checks;

public enum ScenarioType {
  REGULAR_SWB,
  REGULAR_BOL,
  ACTIVE_REEFER,
  NON_OPERATING_REEFER,
  DG,
  REGULAR_2C_2U_1E,
  REGULAR_2C_2U_2E,
  REGULAR_SWB_SOC_AND_REFERENCES,
  REGULAR_SWB_AMF,
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
      case REGULAR_SWB_SOC_AND_REFERENCES -> "soc-references";
      case REGULAR_SWB_AMF -> "amf";
      case REGULAR_SWB, REGULAR_BOL, ACTIVE_REEFER, NON_OPERATING_REEFER, DG -> "request";
    };
    return "ebl-api-" + version.toLowerCase() + "-" + suffix + ".json";
  }

  public boolean isReferencesRequired() {
    return this == REGULAR_SWB_SOC_AND_REFERENCES;
  }

  public boolean isCustomsReferencesRequired() {
    return this == REGULAR_SWB_SOC_AND_REFERENCES;
  }
}
