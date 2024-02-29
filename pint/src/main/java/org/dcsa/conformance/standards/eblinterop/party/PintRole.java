package org.dcsa.conformance.standards.eblinterop.party;

import lombok.Getter;

@Getter
public enum PintRole {
  SENDING_PLATFORM("SendingPlatform"),
  RECEIVING_PLATFORM("ReceivingPlatform");

  private final String configName;

  PintRole(String configName) {
    this.configName = configName;
  }

  public static boolean isSendingPlatform(String configName) {
    return PintRole.SENDING_PLATFORM.configName.equals(configName);
  }

  public static boolean isReceivingPlatform(String configName) {
    return PintRole.RECEIVING_PLATFORM.configName.equals(configName);
  }
}
