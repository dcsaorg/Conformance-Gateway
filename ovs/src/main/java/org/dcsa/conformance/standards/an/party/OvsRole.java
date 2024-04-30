package org.dcsa.conformance.standards.an.party;

import lombok.Getter;

@Getter
public enum OvsRole {
  PUBLISHER("Publisher"),
  SUBSCRIBER("Subscriber");

  private final String configName;

  OvsRole(String configName) {
    this.configName = configName;
  }

  public static boolean isPublisher(String configName) {
    return OvsRole.PUBLISHER.configName.equals(configName);
  }

  public static boolean isSubscriber(String configName) {
    return OvsRole.SUBSCRIBER.configName.equals(configName);
  }
}
