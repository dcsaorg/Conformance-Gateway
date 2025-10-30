package org.dcsa.conformance.standards.portcall.model;

import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.conformance.core.UserFacingException;

@Getter
@RequiredArgsConstructor
public enum PortCallServiceTypeCode {
  // Port Call Services negotiable through an `ERP`-cycle including an `A`, can be one of:
  BERTH("Berth"),
  CARGO_OPERATIONS("Cargo operations"),
  PILOTAGE("Pilotage"),
  TOWAGE("Towage"),
  MOORING("Mooring"),
  BUNKERING("Bunkering"),
  PILOT_BOARDING_PLACE("Pilot Boarding Place"),
  ANCHORAGE("Anchorage"),
  SLUDGE("Sludge"),
  // Port Call Services without `ERP`-cycle having only an `A`, can be one of:
  SEA_PASSAGE("Sea Passage"),
  ALL_FAST("All Fast"),
  GANGWAY_DOWN_AND_SECURE("Gangway down and secure"),
  VESSEL_READY_FOR_CARGO_OPERATIONS("Vessel Ready for cargo operations"),
  VESSEL_READY_TO_SAIL("Vessel Ready to sail"),
  DISCHARGE_CARGO_OPERATIONS("Discharge cargo operations"),
  LOADING_CARGO_OPERATIONS("Loading cargo operations"),
  LASHING("Lashing"),
  SAFETY("Safety - Terminal ready for vessel departure"),
  ANCHORAGE_OPERATIONS("Anchorage Operations"),
  SHORE_POWER("ShorePower"),
  // Port Call Service without an `ERP` and without an `A`, can be one of:
  MOVES("Moves"),
  ;

  private final String fullName;

  public static Set<PortCallServiceTypeCode> getServicesWithERPAndA() {
    return Set.of(
        BERTH,
        CARGO_OPERATIONS,
        PILOTAGE,
        TOWAGE,
        MOORING,
        BUNKERING,
        PILOT_BOARDING_PLACE,
        ANCHORAGE,
        SLUDGE);
  }

  public static Set<PortCallServiceTypeCode> getServicesHavingOnlyA() {
    return Set.of(
        SEA_PASSAGE,
        ALL_FAST,
        GANGWAY_DOWN_AND_SECURE,
        VESSEL_READY_FOR_CARGO_OPERATIONS,
        VESSEL_READY_TO_SAIL,
        DISCHARGE_CARGO_OPERATIONS,
        LOADING_CARGO_OPERATIONS,
        LASHING,
        SAFETY,
        ANCHORAGE_OPERATIONS,
        SHORE_POWER);
  }

  public static PortCallServiceTypeCode fromName(String name) {
    for (PortCallServiceTypeCode code : values()) {
      if (code.name().equals(name)) {
        return code;
      }
    }
    throw new UserFacingException("Unknown PortCallServiceTypeCode: " + name);
  }
}
