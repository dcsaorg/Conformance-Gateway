package org.dcsa.conformance.standards.eblsurrender.v10.party;

import lombok.Getter;

@Getter
public enum EblSurrenderV10Role {
  CARRIER("Carrier"),
  PLATFORM("Platform");

  private final String configName;

  EblSurrenderV10Role(String configName) {
    this.configName = configName;
  }

  public static boolean isCarrier(String configName) {
    return EblSurrenderV10Role.CARRIER.configName.equals(configName);
  }

  public static boolean isPlatform(String configName) {
    return EblSurrenderV10Role.PLATFORM.configName.equals(configName);
  }
}
