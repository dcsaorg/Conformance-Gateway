package org.dcsa.conformance.standards.eblsurrender.party;

import lombok.Getter;

@Getter
public enum EblSurrenderRole {
  CARRIER("Carrier"),
  PLATFORM("Platform");

  private final String configName;

  EblSurrenderRole(String configName) {
    this.configName = configName;
  }

  public static boolean isCarrier(String configName) {
    return EblSurrenderRole.CARRIER.configName.equals(configName);
  }

  public static boolean isPlatform(String configName) {
    return EblSurrenderRole.PLATFORM.configName.equals(configName);
  }
}
