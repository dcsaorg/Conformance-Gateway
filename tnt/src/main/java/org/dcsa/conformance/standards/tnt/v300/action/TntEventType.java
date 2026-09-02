package org.dcsa.conformance.standards.tnt.v300.action;

import org.dcsa.conformance.standards.tnt.v300.party.TntQueryParameters;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    return "tnt-" + version + "-request-" + suffix + ".json";
  }


  public Set<TntQueryParameters> applicableBaseFilters() {
    List<TntQueryParameters> baseFilters =
      switch (this) {
        case SHIPMENT, TRANSPORT -> List.of(TntQueryParameters.CBR, TntQueryParameters.TDR);
        case EQUIPMENT -> List.of(TntQueryParameters.CBR, TntQueryParameters.TDR, TntQueryParameters.ER);
        case IOT, REEFER -> List.of(TntQueryParameters.ER);
      };
    return new LinkedHashSet<>(baseFilters);
  }
}
