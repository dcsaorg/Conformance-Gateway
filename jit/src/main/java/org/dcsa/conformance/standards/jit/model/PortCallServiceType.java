package org.dcsa.conformance.standards.jit.model;

import lombok.Getter;

import java.util.List;

public enum PortCallServiceType {
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
  VESSEL_READY_FOR_CARGO("Vessel Ready for cargo operations"),
  VESSEL_READY_TO_SAIL("Vessel Ready to sail"), // TODO: update later
  DISCHARGE_CARGO_OPERATIONS("Discharge cargo operations"),
  LOADING_CARGO_OPERATIONS("Loading cargo operations"),
  LASHING("Lashing"),
  SAFETY("Safety - Terminal ready for vessel departure"),
  ANCHORAGE_OPERATIONS("Anchorage Operations"),
  SHORE_POWER("ShorePower"),
  // Port Call Service without an `ERP` and without an `A`, can be one of:
  MOVES("Moves forecast"),
  CANCEL("Cancel"),
  DECLINE("Decline"),
  OMISSION("Omission"),
  ;

  @Getter private final String fullName;

  PortCallServiceType(String fullName) {
    this.fullName = fullName;
  }

  public static List<PortCallServiceType> getServicesWithERPAndA() {
    return List.of(
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

  public static List<PortCallServiceType> getServicesWithoutERPHavingOnlyA() {
    return List.of(
        SEA_PASSAGE,
        ALL_FAST,
        GANGWAY_DOWN_AND_SECURE,
        VESSEL_READY_FOR_CARGO,
        VESSEL_READY_TO_SAIL,
        DISCHARGE_CARGO_OPERATIONS,
        LOADING_CARGO_OPERATIONS,
        LASHING,
        SAFETY,
        ANCHORAGE_OPERATIONS,
        SHORE_POWER);
  }

  public static PortCallServiceType fromName(String name) {
    for (PortCallServiceType portCallServiceType : values()) {
      if (portCallServiceType.name().equals(name)) {
        return portCallServiceType;
      }
    }
    return null;
  }
}
