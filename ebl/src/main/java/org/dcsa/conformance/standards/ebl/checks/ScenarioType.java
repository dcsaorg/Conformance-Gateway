package org.dcsa.conformance.standards.ebl.checks;

public enum ScenarioType {
  REGULAR_SWB,
  REGULAR_STRAIGHT_BL,
  REGULAR_NEGOTIABLE_BL,
  ACTIVE_REEFER,
  NON_OPERATING_REEFER,
  DG,
  REGULAR_2C_1U,
  REGULAR_2C_2U,
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

  public String eblPayload(String version) {
    String suffix =
        switch (this) {
          case REGULAR_2C_1U -> "2c1u";
          case REGULAR_2C_2U -> "2c2u";
          case REGULAR_NO_COMMODITY_SUBREFERENCE -> "no-commodity-subreference";
          case REGULAR_SWB_SOC_AND_REFERENCES -> "soc-references";
          case REGULAR_SWB_AMF -> "amf";
          case DG -> "dg";
          case REGULAR_SWB, ACTIVE_REEFER, NON_OPERATING_REEFER -> "request";
          case REGULAR_STRAIGHT_BL -> "straight-bl";
          case REGULAR_NEGOTIABLE_BL -> "negotiable-bl";
          case REGULAR_CLAD -> "clad";
        };
    return "ebl-api-" + version.toLowerCase() + "-" + suffix + ".json";
  }

  public String getTDScenarioTypeName() {
    return switch (this) {
      case REGULAR_SWB -> "SWB";
      case REGULAR_STRAIGHT_BL -> "STRAIGHT_BL";
      case REGULAR_NEGOTIABLE_BL -> "NEGOTIABLE_BL";
      case REGULAR_2C_1U -> "2C_1U";
      case REGULAR_2C_2U -> "2C_2U";
      case REGULAR_SWB_SOC_AND_REFERENCES -> "SOC";
      case REGULAR_SWB_AMF -> "AMF";
      case DG -> "DG";
      case ACTIVE_REEFER -> "ACTIVE_REEFER";
      case NON_OPERATING_REEFER -> "NON_OPERATING_REEFER";
      case REGULAR_CLAD -> "CLAD";
      default -> "";
    };
  }
}
