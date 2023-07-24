package org.dcsa.conformance.gateway.standards.eblsurrender.v10;

import lombok.Getter;

import java.util.Map;
import java.util.TreeMap;

enum EblSurrenderRole {
  CARRIER("Carrier"),
  PLATFORM("Platform");

  private static final Map<String, EblSurrenderRole> rolesByConfigName = new TreeMap<>();

  @Getter private final String configName;

  private EblSurrenderRole(String configName) {
    this.configName = configName;
  }

  static EblSurrenderRole forConfigName(String configName) {
    return rolesByConfigName.get(configName);
  }

  static boolean isCarrier(String configName) {
    return EblSurrenderRole.CARRIER.equals(EblSurrenderRole.forConfigName(configName));
  }

  static boolean isPlatform(String configName) {
    return EblSurrenderRole.PLATFORM.equals(EblSurrenderRole.forConfigName(configName));
  }
}
