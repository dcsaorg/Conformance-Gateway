package org.dcsa.conformance.standards.tnt.v300.action;

public enum TntEventType {
  SHIPMENT,
  TRANSPORT,
  EQUIPMENT,
  IOT,
  REEFER;

  public String tntEventPayload(String version) {
    String suffix =
        switch (this) {
          case SHIPMENT -> "shipment";
          case TRANSPORT -> "transport";
          case EQUIPMENT -> "equipment";
          case IOT -> "iot";
          case REEFER -> "reefer";
        };
    return "tnt-" + version + "-" + suffix + "-request.json";
  }

  public String tntEventResponse(String version) {
    String suffix =
        switch (this) {
          case SHIPMENT -> "shipment";
          case TRANSPORT -> "transport";
          case EQUIPMENT -> "equipment";
          case IOT -> "iot";
          case REEFER -> "reefer";
        };
    return "tnt-" + version + "-" + suffix + "-response.json";
  }
}
