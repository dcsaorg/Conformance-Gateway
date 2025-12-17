package org.dcsa.conformance.standards.portcall.party;

import lombok.Getter;

@Getter
public enum PortCallRole {
  PUBLISHER("Publisher"),
  SUBSCRIBER("Subscriber");

  private final String configName;

  PortCallRole(String configName) {
    this.configName = configName;
  }

  public static boolean isPublisher(String configName) {
    return PortCallRole.PUBLISHER.configName.equals(configName);
  }

  public static boolean isSubscriber(String configName) {
    return PortCallRole.SUBSCRIBER.configName.equals(configName);
  }
}
