package org.dcsa.conformance.gateway.standards.eblsurrender.v10;

import lombok.Getter;

@Getter
enum EblSurrenderV10Role {
  CARRIER("Carrier"),
  PLATFORM("Platform");

  private final String configName;

  EblSurrenderV10Role(String configName) {
    this.configName = configName;
  }

  static boolean isCarrier(String configName) {
    return EblSurrenderV10Role.CARRIER.configName.equals(configName);
  }

  static boolean isPlatform(String configName) {
    return EblSurrenderV10Role.PLATFORM.configName.equals(configName);
  }
}
