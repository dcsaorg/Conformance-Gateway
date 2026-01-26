package org.dcsa.conformance.end.party;

import lombok.Getter;

@Getter
public enum EndorsementChainRole {
  CARRIER("Carrier"),
  PROVIDER("Provider");

  private final String configName;

  EndorsementChainRole(String configName) {
    this.configName = configName;
  }

  public static boolean isCarrier(String configName) {
    return EndorsementChainRole.CARRIER.configName.equals(configName);
  }

  public static boolean isProvider(String configName) {
    return EndorsementChainRole.PROVIDER.configName.equals(configName);
  }
}

