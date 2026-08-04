package org.dcsa.conformance.standards.ovs.party;

import lombok.Getter;

@Getter
public enum OvsRole {
  PRODUCER("Schedule Producer"),
  CONSUMER("Schedule Consumer");

  private final String configName;

  OvsRole(String configName) {
    this.configName = configName;
  }

  public static boolean isPublisher(String configName) {
    return OvsRole.PRODUCER.configName.equals(configName);
  }

  public static boolean isSubscriber(String configName) {
    return OvsRole.CONSUMER.configName.equals(configName);
  }
}
