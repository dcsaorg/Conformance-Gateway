package org.dcsa.conformance.standards.ebl.checks;

public enum ScenarioType {
  REGULAR_SWB,
  REGULAR_STRAIGHT_BL,
  REGULAR_NEGOTIABLE_BL,
  ACTIVE_REEFER,
  NON_OPERATING_REEFER,
  DG,
  REGULAR_2C_2U_1E,
  REGULAR_2C_2U_2E,
  REGULAR_NO_COMMODITY_SUBREFERENCE,
  REGULAR_SWB_SOC_AND_REFERENCES,
  REGULAR_SWB_AMF,
  REGULAR_CLAD,
  ;

  public String transportDocumentTypeCode() {
    return this == REGULAR_STRAIGHT_BL || this == REGULAR_NEGOTIABLE_BL ? "BOL" : "SWB";
  }

  public boolean isToOrder() {
    return this == REGULAR_NEGOTIABLE_BL;
  }

  public String shipperTemplate(String version) {
    String suffix = switch (this) {
      case REGULAR_2C_2U_1E -> "2c2u1e";
      case REGULAR_2C_2U_2E -> "2c2u2e";
      case REGULAR_NO_COMMODITY_SUBREFERENCE -> "no-commodity-subreference";
      case REGULAR_SWB_SOC_AND_REFERENCES -> "soc-references";
      case REGULAR_SWB_AMF -> "amf";
      case DG -> "dg";
      case REGULAR_SWB, REGULAR_STRAIGHT_BL, ACTIVE_REEFER, NON_OPERATING_REEFER -> "request";
      case REGULAR_NEGOTIABLE_BL -> "negotiable-bl";
      case REGULAR_CLAD -> "clad";
    };
    return "ebl-api-" + version.toLowerCase() + "-" + suffix + ".json";
  }

  public boolean isReferencesRequired() {
    return this == REGULAR_SWB_SOC_AND_REFERENCES;
  }

  public boolean isCustomsReferencesRequired() {
    return this == REGULAR_SWB_SOC_AND_REFERENCES;
  }

  public boolean hasDG() {
    return this == DG;
  }

  public boolean isCarriersAgentAtDestinationRequired() {
    return this == REGULAR_CLAD;
  }

  public boolean isSWB() {
    return this == REGULAR_SWB || this == REGULAR_SWB_AMF || this == REGULAR_SWB_SOC_AND_REFERENCES;
  }
}
