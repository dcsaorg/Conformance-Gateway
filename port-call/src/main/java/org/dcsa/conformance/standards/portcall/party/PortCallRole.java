package org.dcsa.conformance.standards.portcall.party;

import lombok.Getter;

@Getter
public enum PortCallRole {
  PRODUCER("Producer"),
  CONSUMER("Consumer");

  private final String configName;

  PortCallRole(String configName) {
    this.configName = configName;
  }

  public static boolean isProducer(String configName) {
    return PortCallRole.PRODUCER.configName.equals(configName);
  }

  public static boolean isConsumer(String configName) {
    return PortCallRole.CONSUMER.configName.equals(configName);
  }
}
