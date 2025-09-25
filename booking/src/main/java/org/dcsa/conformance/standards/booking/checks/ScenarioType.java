package org.dcsa.conformance.standards.booking.checks;

public enum ScenarioType {
  REGULAR,
  ROUTING_REFERENCE,
  STORE_DOOR_AT_ORIGIN,
  STORE_DOOR_AT_DESTINATION,
  NON_OPERATING_REEFER,
  REEFER,
  DG;

  public String bookingPayload(String version) {
    String suffix =
        switch (this) {
          case REEFER -> "reefer";
          case NON_OPERATING_REEFER -> "non-operating-reefer";
          case DG -> "dg";
          case ROUTING_REFERENCE -> "routing-reference";
          case STORE_DOOR_AT_ORIGIN -> "store-door-at-origin";
          case STORE_DOOR_AT_DESTINATION -> "store-door-at-destination";
          default -> "regular";
        };
    return "booking-api-" + version.toLowerCase() + "-" + suffix + ".json";
  }
}
