package org.dcsa.conformance.standards.ovs.party;

import lombok.Getter;

@Getter
public enum OvsRole {

  PRODUCER("Producer"),
  CONSUMER("Consumer");

  private final String configName;

  OvsRole(String configName) {
    this.configName = configName;
  }

  public static boolean isProducer(String configName) {
    return OvsRole.PRODUCER.configName.equals(configName);
  }

  public static boolean isConsumer(String configName) {
    return OvsRole.CONSUMER.configName.equals(configName);
  }
}
