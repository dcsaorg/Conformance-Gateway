package org.dcsa.conformance.standards.cs.party;

import lombok.Getter;

@Getter
public enum CsRole {
  PUBLISHER("Publisher"),
  SUBSCRIBER("Subscriber");

  private final String configName;

  CsRole(String configName) {
    this.configName = configName;
  }

  public static boolean isPublisher(String configName) {
    return CsRole.PUBLISHER.configName.equals(configName);
  }

  public static boolean isSubscriber(String configName) {
    return CsRole.SUBSCRIBER.configName.equals(configName);
  }
}
