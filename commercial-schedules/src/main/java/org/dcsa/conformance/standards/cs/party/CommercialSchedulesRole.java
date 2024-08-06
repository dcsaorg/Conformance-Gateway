package org.dcsa.conformance.standards.cs.party;

public enum CommercialSchedulesRole {

  PUBLISHER("Publisher"),
  SUBSCRIBER("Subscriber");

  private final String configName;

  CommercialSchedulesRole(String configName) {
    this.configName = configName;
  }

  public static boolean isCarrier(String configName) {
    return CommercialSchedulesRole.PUBLISHER.configName.equals(configName);
  }

  public static boolean isShipper(String configName) {
    return CommercialSchedulesRole.SUBSCRIBER.configName.equals(configName);
  }
}
