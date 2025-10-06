package org.dcsa.conformance.standards.bookingandebl.party;

import lombok.Getter;

@Getter
public enum BookingAndEblRole {
  CARRIER("Carrier"),
  SHIPPER("Shipper");

  private final String configName;

  BookingAndEblRole(String configName) {
    this.configName = configName;
  }

  public static boolean isCarrier(String configName) {
    return BookingAndEblRole.CARRIER.configName.equals(configName);
  }

  public static boolean isShipper(String configName) {
    return BookingAndEblRole.SHIPPER.configName.equals(configName);
  }
}
