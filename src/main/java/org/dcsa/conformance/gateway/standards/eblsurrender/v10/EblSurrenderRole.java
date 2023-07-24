package org.dcsa.conformance.gateway.standards.eblsurrender.v10;

import lombok.Getter;

import java.util.Map;
import java.util.TreeMap;

enum EblSurrenderRole {
  CARRIER("Carrier"),
  PLATFORM("Platform");

  @Getter private final String configName;

  private EblSurrenderRole(String configName) {
    this.configName = configName;
  }

  static boolean isCarrier(String configName) {
    return EblSurrenderRole.CARRIER.configName.equals(configName);
  }

  static boolean isPlatform(String configName) {
    return EblSurrenderRole.PLATFORM.configName.equals(configName);
  }
}
