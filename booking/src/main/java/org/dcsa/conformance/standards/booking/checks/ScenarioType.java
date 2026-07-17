package org.dcsa.conformance.standards.booking.checks;

public enum ScenarioType {
  ANY("dry-cargo", "any BKG", false),
  DRY_CARGO("dry-cargo", "Dry cargo", true),
  ROUTING_REFERENCE("routing-reference", "Routing reference", true),
  STORE_DOOR_AT_ORIGIN("store-door-at-origin", "Store door at origin", true),
  STORE_DOOR_AT_DESTINATION("store-door-at-destination", "Store door at destination", true),
  NON_OPERATING_REEFER("non-operating-reefer", "Non-operating reefer", true),
  REEFER("reefer", "Reefer", true),
  DG("dg", "DG", true);

  private final String payloadSuffix;
  private final String displayName;
  private final boolean cargoTypeValidationRequired;

  ScenarioType(
      String payloadSuffix, String displayName, boolean cargoTypeValidationRequired) {
    this.payloadSuffix = payloadSuffix;
    this.displayName = displayName;
    this.cargoTypeValidationRequired = cargoTypeValidationRequired;
  }

  public String bookingPayload(String version) {
    return "booking-api-" + version.toLowerCase() + "-" + payloadSuffix + ".json";
  }

  public String displayName() {
    return displayName;
  }

  public boolean isCargoTypeValidationRequired() {
    return cargoTypeValidationRequired;
  }
}
