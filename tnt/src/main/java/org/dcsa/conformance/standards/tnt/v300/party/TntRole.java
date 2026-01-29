package org.dcsa.conformance.standards.tnt.v300.party;

import lombok.Getter;

@Getter
public enum TntRole {
  PRODUCER("Producer"),
  CONSUMER("Consumer");

  private final String configName;

  TntRole(String configName) {
    this.configName = configName;
  }

  public static boolean isProducer(String configName) {
    return TntRole.PRODUCER.configName.equals(configName);
  }

  public static boolean isConsumer(String configName) {
    return TntRole.CONSUMER.configName.equals(configName);
  }
}
