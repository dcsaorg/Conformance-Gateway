package org.dcsa.conformance.standards.portcall.party;

import lombok.Getter;

@Getter
public enum JitRole {
  PROVIDER("Service Provider"),
  CONSUMER("Service Consumer");

  private final String configName;

  JitRole(String configName) {
    this.configName = configName;
  }

  public static boolean isProvider(String configName) {
    return JitRole.PROVIDER.configName.equals(configName);
  }

  public static boolean isConsumer(String configName) {
    return JitRole.CONSUMER.configName.equals(configName);
  }
}
