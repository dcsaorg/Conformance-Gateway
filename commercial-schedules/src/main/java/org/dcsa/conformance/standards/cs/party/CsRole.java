package org.dcsa.conformance.standards.cs.party;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

  public static Set<String> getRoleNames() {
    return Arrays.stream(CsRole.values()).map(CsRole::getConfigName).collect(Collectors.toSet());
  }
}
