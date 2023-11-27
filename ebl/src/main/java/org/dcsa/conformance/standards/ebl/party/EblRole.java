package org.dcsa.conformance.standards.ebl.party;

import lombok.Getter;

@Getter
public enum EblRole {
  CARRIER("Carrier"),
  SHIPPER("Shipper");

  private final String configName;

  EblRole(String configName) {
    this.configName = configName;
  }

  public static boolean isCarrier(String configName) {
    return EblRole.CARRIER.configName.equals(configName);
  }

  public static boolean isShipper(String configName) {
    return EblRole.SHIPPER.configName.equals(configName);
  }
}
