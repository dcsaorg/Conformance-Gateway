package org.dcsa.conformance.standards.booking.checks;

public enum ScenarioType {
  DRY_CARGO("dry-cargo", "Dry cargo"),
  ROUTING_REFERENCE("routing-reference", "Routing reference"),
  STORE_DOOR_AT_ORIGIN("store-door-at-origin", "Store door at origin"),
  STORE_DOOR_AT_DESTINATION("store-door-at-destination", "Store door at destination"),
  NON_OPERATING_REEFER("non-operating-reefer", "Non-operating reefer"),
  REEFER("reefer", "Reefer"),
  DG("dg", "DG");

  private final String payloadSuffix;
  private final String displayName;

  ScenarioType(String payloadSuffix, String displayName) {
    this.payloadSuffix = payloadSuffix;
    this.displayName = displayName;
  }

  public String bookingPayload(String version) {
    return "booking-api-" + version.toLowerCase() + "-" + payloadSuffix + ".json";
  }

  public String displayName() {
    return displayName;
  }
}
