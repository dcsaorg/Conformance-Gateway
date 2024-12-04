package org.dcsa.conformance.standards.jit.model;

import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.*;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import org.dcsa.conformance.core.UserFacingException;

@Getter
public enum PortCallServiceEventTypeCode {
  STRT("Started"),
  CMPL("Completed"),
  ARRI("Arrived"),
  DEPA("Departed");

  private final String fullName;

  PortCallServiceEventTypeCode(String fullName) {
    this.fullName = fullName;
  }

  public static List<PortCallServiceType> getValidPortCallServiceTypes(PortCallServiceEventTypeCode code) {
    return switch (code) {
      case STRT, CMPL ->
          List.of(
              CARGO_OPERATIONS,
              PILOTAGE,
              TOWAGE,
              MOORING,
              BUNKERING,
              ANCHORAGE,
              SLUDGE,
              SEA_PASSAGE,
              DISCHARGE_CARGO_OPERATIONS,
              LOADING_CARGO_OPERATIONS,
              LASHING,
              ANCHORAGE_OPERATIONS,
              SHORE_POWER);
      case ARRI ->
          List.of(
              BERTH,
              PILOT_BOARDING_PLACE,
              ALL_FAST,
              GANGWAY_DOWN_AND_SECURE,
              VESSEL_READY_FOR_CARGO_OPERATIONS,
              VESSEL_READY_TO_SAIL,
              MOVES);
      case DEPA -> List.of(SAFETY, BERTH);
    };
  }

  public static List<PortCallServiceEventTypeCode> getCodesForPortCallServiceType(
      String portCallServiceType) {
    PortCallServiceType givenCode = PortCallServiceType.fromName(portCallServiceType);
    return Arrays.stream(PortCallServiceEventTypeCode.values())
        .filter(code -> getValidPortCallServiceTypes(code).contains(givenCode))
        .toList();
  }

  public static PortCallServiceEventTypeCode fromString(String name) {
    for (PortCallServiceEventTypeCode eventTypeCode : PortCallServiceEventTypeCode.values()) {
      if (eventTypeCode.name().equals(name)) {
        return eventTypeCode;
      }
    }
    throw new UserFacingException("Unknown PortCallServiceEventTypeCode: " + name);
  }
}
