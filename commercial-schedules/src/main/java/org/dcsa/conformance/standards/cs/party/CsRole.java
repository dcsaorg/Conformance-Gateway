package org.dcsa.conformance.standards.cs.party;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum CsRole {
  PRODUCER("Producer"),
  CONSUMER("Consumer");

  private final String configName;

  CsRole(String configName) {
    this.configName = configName;
  }

  public static boolean isProducer(String configName) {
    return CsRole.PRODUCER.configName.equals(configName);
  }

  public static boolean isConsumer(String configName) {
    return CsRole.CONSUMER.configName.equals(configName);
  }

  public static Set<String> getRoleNames() {
    return Arrays.stream(CsRole.values()).map(CsRole::getConfigName).collect(Collectors.toSet());
  }
}
