package org.dcsa.conformance.standards.an.party;

import lombok.Getter;

@Getter
public enum ArrivalNoticeRole {
  CARRIER("CARRIER"),
  NOTIFYPARTY("NOTIFYPARTY");

  private final String configName;

  ArrivalNoticeRole(String configName) {
    this.configName = configName;
  }

  public static boolean isCarrier(String configName) {
    return ArrivalNoticeRole.CARRIER.configName.equals(configName);
  }

  public static boolean isNotifyParty(String configName) {
    return ArrivalNoticeRole.NOTIFYPARTY.configName.equals(configName);
  }
}
