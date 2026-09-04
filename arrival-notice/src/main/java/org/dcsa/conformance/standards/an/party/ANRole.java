package org.dcsa.conformance.standards.an.party;

import lombok.Getter;

@Getter
public enum ANRole {
  PRODUCER("Producer"),
  CONSUMER("Consumer");

  private final String configName;

  ANRole(String configName) {
    this.configName = configName;
  }

  public static boolean isProducer(String configName) {
    return ANRole.PRODUCER.configName.equals(configName);
  }

  public static boolean isConsumer(String configName) {
    return ANRole.CONSUMER.configName.equals(configName);
  }
}
