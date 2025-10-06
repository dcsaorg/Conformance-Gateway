package org.dcsa.conformance.standards.an.party;

import lombok.Getter;

@Getter
public enum ANRole {
  PUBLISHER("Publisher"),
  SUBSCRIBER("Subscriber");

  private final String configName;

  ANRole(String configName) {
    this.configName = configName;
  }

  public static boolean isPublisher(String configName) {
    return ANRole.PUBLISHER.configName.equals(configName);
  }

  public static boolean isSubscriber(String configName) {
    return ANRole.SUBSCRIBER.configName.equals(configName);
  }
}
