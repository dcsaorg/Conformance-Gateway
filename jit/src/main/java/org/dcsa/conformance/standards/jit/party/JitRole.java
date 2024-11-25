package org.dcsa.conformance.standards.jit.party;

import lombok.Getter;

@Getter
public enum JitRole {
  PROVIDER("Provider"),
  CONSUMER("Consumer");

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
