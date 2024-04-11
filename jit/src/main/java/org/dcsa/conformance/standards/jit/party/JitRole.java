package org.dcsa.conformance.standards.jit.party;

import lombok.Getter;

@Getter
public enum JitRole {
  PUBLISHER("Publisher"),
  SUBSCRIBER("Subscriber");

  private final String configName;

  JitRole(String configName) {
    this.configName = configName;
  }

  public static boolean isPublisher(String configName) {
    return JitRole.PUBLISHER.configName.equals(configName);
  }

  public static boolean isSubscriber(String configName) {
    return JitRole.SUBSCRIBER.configName.equals(configName);
  }
}
