package org.dcsa.conformance.standards.booking.checks;

import lombok.Getter;

@Getter
public enum ScenarioType {
  ANY("dry-cargo", "any BKG", false),
  DRY_CARGO("dry-cargo", "Dry cargo", true),
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
}
