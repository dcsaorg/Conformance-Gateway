package org.dcsa.conformance.standards.booking.checks;

public enum ScenarioType {
  REGULAR,
  NON_OPERATING_REEFER,
  REEFER,
  DG;

  public String bookingPayload(String version) {
    String suffix =
        switch (this) {
          case REEFER -> "reefer";
          case NON_OPERATING_REEFER -> "non-operating-reefer";
          case DG -> "dg";
          default -> "regular";
        };
    return "booking-api-" + version.toLowerCase() + "-" + suffix + ".json";
  }
}
