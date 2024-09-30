package org.dcsa.conformance.standards.adoption.party;

import lombok.Getter;

@Getter
public enum AdoptionRole {
  ADOPTER("Adopter"),
  DCSA("DCSA");

  private final String configName;

  AdoptionRole(String configName) {
    this.configName = configName;
  }

  public static boolean isAdopter(String configName) {
    return AdoptionRole.ADOPTER.configName.equals(configName);
  }

  public static boolean isDCSA(String configName) {
    return AdoptionRole.DCSA.configName.equals(configName);
  }
}
