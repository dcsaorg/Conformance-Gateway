package org.dcsa.conformance.standards.tnt.party;

import lombok.Getter;

@Getter
public enum TntRole {
  PUBLISHER("Publisher"),
  SUBSCRIBER("Subscriber");

  private final String configName;

  TntRole(String configName) {
    this.configName = configName;
  }

  public static boolean isPublisher(String configName) {
    return TntRole.PUBLISHER.configName.equals(configName);
  }

  public static boolean isSubscriber(String configName) {
    return TntRole.SUBSCRIBER.configName.equals(configName);
  }
}
