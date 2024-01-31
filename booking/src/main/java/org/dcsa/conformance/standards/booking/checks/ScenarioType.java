package org.dcsa.conformance.standards.booking.checks;

public enum ScenarioType {
  REGULAR,
  REGULAR_2RE1C,
  REGULAR_2RE2C,
  REGULAR_CHO_DEST,
  REGULAR_CHO_ORIG,
  REGULAR_NON_OPERATING_REEFER,
  REGULAR_SHIPPER_OWNED,
  REEFER,
  REEFER_TEMP_CHANGE,
  DG;

  public String bookingTemplate(String version) {
    String suffix = switch (this) {
      case REGULAR_2RE1C -> "regular-2re1c";
      case REGULAR_2RE2C -> "regular-2re2c";
      case REGULAR_CHO_DEST -> "regular-cho-dest";
      case REGULAR_CHO_ORIG -> "regular-cho-orig";
      case REGULAR_NON_OPERATING_REEFER -> "regular-non-operating-reefer";
      case REGULAR_SHIPPER_OWNED -> "regular-shipper-owned";
      case REEFER_TEMP_CHANGE -> "reefer-temp-change";
      case REEFER -> "reefer";
      case REGULAR -> "regular";
      case DG -> "dg";
    };
    return "booking-api-" + version.toLowerCase() + "-" + suffix + ".json";
  }
}
