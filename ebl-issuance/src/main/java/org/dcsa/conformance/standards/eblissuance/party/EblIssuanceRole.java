package org.dcsa.conformance.standards.eblissuance.party;

import lombok.Getter;

@Getter
public enum EblIssuanceRole {
  CARRIER("Carrier"),
  PLATFORM("Platform");

  private final String configName;

  EblIssuanceRole(String configName) {
    this.configName = configName;
  }

  public static boolean isCarrier(String configName) {
    return EblIssuanceRole.CARRIER.configName.equals(configName);
  }

  public static boolean isPlatform(String configName) {
    return EblIssuanceRole.PLATFORM.configName.equals(configName);
  }
}
