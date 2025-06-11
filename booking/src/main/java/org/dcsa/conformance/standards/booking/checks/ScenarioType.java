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

  public String bookingPayload(String version) {
    String suffix =
        switch (this) {
          case REGULAR_2RE1C,
              REGULAR_2RE2C,
              REGULAR_CHO_DEST,
              REGULAR_CHO_ORIG,
              REGULAR_NON_OPERATING_REEFER,
              REGULAR_SHIPPER_OWNED,
              REGULAR ->
              "regular";
          case REEFER_TEMP_CHANGE, REEFER -> "reefer";
          case DG -> "dg";
        };
    return "booking-api-" + version.toLowerCase() + "-" + suffix + ".json";
  }
}
