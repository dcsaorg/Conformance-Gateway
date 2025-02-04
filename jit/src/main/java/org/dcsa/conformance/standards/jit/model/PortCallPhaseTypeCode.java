package org.dcsa.conformance.standards.jit.model;

import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.ALL_FAST;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.ANCHORAGE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.ANCHORAGE_OPERATIONS;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.BERTH;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.BUNKERING;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.CARGO_OPERATIONS;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.DISCHARGE_CARGO_OPERATIONS;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.GANGWAY_DOWN_AND_SECURE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.LASHING;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.LOADING_CARGO_OPERATIONS;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.MOORING;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.MOVES;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.PILOTAGE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.PILOT_BOARDING_PLACE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.SAFETY;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.SEA_PASSAGE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.SHORE_POWER;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.SLUDGE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.TOWAGE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.VESSEL_READY_FOR_CARGO_OPERATIONS;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.VESSEL_READY_TO_SAIL;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public enum PortCallPhaseTypeCode {
  INBD, // (Inbound)
  ALGS, // (Alongside)
  SHIF, // (Shifting)
  OUTB, // (Outbound)
  ;

  // Some PortCallServiceType codes are not associated with a PortCallPhaseTypeCode (keep empty)
  public static final List<PortCallServiceType> EMPTY_PHASE_TYPE_CODES =
      List.of(ANCHORAGE, SLUDGE, ANCHORAGE_OPERATIONS, MOVES);

  public static Set<PortCallServiceType> getValidPortCallPhaseTypeCode(PortCallPhaseTypeCode code) {
    return switch (code) {
      case INBD -> Set.of(BERTH, PILOTAGE, TOWAGE, MOORING, PILOT_BOARDING_PLACE, SEA_PASSAGE);
      case ALGS ->
          Set.of(
              BERTH,
              BUNKERING,
              CARGO_OPERATIONS,
              ALL_FAST,
              GANGWAY_DOWN_AND_SECURE,
              VESSEL_READY_FOR_CARGO_OPERATIONS,
              VESSEL_READY_TO_SAIL,
              DISCHARGE_CARGO_OPERATIONS,
              LOADING_CARGO_OPERATIONS,
              LASHING,
              SAFETY,
              SHORE_POWER);
      case SHIF -> Set.of(PILOTAGE, TOWAGE, MOORING);
      case OUTB -> Set.of(BERTH, PILOTAGE, TOWAGE, MOORING, SEA_PASSAGE);
    };
  }

  public static List<PortCallPhaseTypeCode> getCodesForPortCallServiceType(
      String portCallServiceType) {
    PortCallServiceType serviceType = PortCallServiceType.fromName(portCallServiceType);
    return Arrays.stream(PortCallPhaseTypeCode.values())
        .filter(code -> getValidPortCallPhaseTypeCode(code).contains(serviceType))
        .toList();
  }
}
