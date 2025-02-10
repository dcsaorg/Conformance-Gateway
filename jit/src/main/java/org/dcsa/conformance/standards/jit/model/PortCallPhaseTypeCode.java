package org.dcsa.conformance.standards.jit.model;

import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.ALL_FAST;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.BERTH;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.BUNKERING;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.CARGO_OPERATIONS;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.DISCHARGE_CARGO_OPERATIONS;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.GANGWAY_DOWN_AND_SECURE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.LASHING;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.LOADING_CARGO_OPERATIONS;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.MOORING;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.PILOTAGE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.PILOT_BOARDING_PLACE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.SAFETY;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.SEA_PASSAGE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.SHORE_POWER;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.TOWAGE;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.VESSEL_READY_FOR_CARGO_OPERATIONS;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.VESSEL_READY_TO_SAIL;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public enum PortCallPhaseTypeCode {
  INBD, // (Inbound)
  ALGS, // (Alongside)
  SHIF, // (Shifting)
  OUTB, // (Outbound)
  ;

  public static Set<PortCallServiceTypeCode> getValidPortCallPhaseTypeCode(
      PortCallPhaseTypeCode code) {
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
    PortCallServiceTypeCode serviceType = PortCallServiceTypeCode.fromName(portCallServiceType);
    return Arrays.stream(PortCallPhaseTypeCode.values())
        .filter(code -> getValidPortCallPhaseTypeCode(code).contains(serviceType))
        .toList();
  }
}
