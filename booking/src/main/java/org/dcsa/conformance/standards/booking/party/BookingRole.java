package org.dcsa.conformance.standards.booking.party;

import lombok.Getter;

@Getter
public enum BookingRole {
  CARRIER("Carrier"),
  SHIPPER("Shipper");

  private final String configName;

  BookingRole(String configName) {
    this.configName = configName;
  }

  public static boolean isCarrier(String configName) {
    return BookingRole.CARRIER.configName.equals(configName);
  }

  public static boolean isShipper(String configName) {
    return BookingRole.SHIPPER.configName.equals(configName);
  }
}
