package org.dcsa.conformance.end.party;

import lombok.Getter;

@Getter
public enum EndorsementChainRole {
  CONSUMER("Consumer"),
  PROVIDER("Provider");

  private final String configName;

  EndorsementChainRole(String configName) {
    this.configName = configName;
  }

  public static boolean isConsumer(String configName) {
    return EndorsementChainRole.CONSUMER.configName.equals(configName);
  }

  public static boolean isProvider(String configName) {
    return EndorsementChainRole.PROVIDER.configName.equals(configName);
  }
}

