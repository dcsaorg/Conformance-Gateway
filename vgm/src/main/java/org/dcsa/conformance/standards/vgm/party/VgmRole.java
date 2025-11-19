package org.dcsa.conformance.standards.vgm.party;

import lombok.Getter;

@Getter
public enum VgmRole {
  PRODUCER("Producer"),
  CONSUMER("Consumer");

  private final String configName;

  VgmRole(String configName) {
    this.configName = configName;
  }

  public static boolean isProducer(String configName) {
    return VgmRole.PRODUCER.configName.equals(configName);
  }

  public static boolean isConsumer(String configName) {
    return VgmRole.CONSUMER.configName.equals(configName);
  }
}
