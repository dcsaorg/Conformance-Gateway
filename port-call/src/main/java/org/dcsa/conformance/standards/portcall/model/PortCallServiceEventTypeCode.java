package org.dcsa.conformance.standards.portcall.model;

import static org.dcsa.conformance.standards.portcall.model.PortCallServiceTypeCode.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.conformance.core.UserFacingException;

@Getter
@RequiredArgsConstructor
public enum PortCallServiceEventTypeCode {
  STRT("Started"),
  CMPL("Completed"),
  ARRI("Arrived"),
  DEPA("Departed");

  private final String fullName;

  public static Set<PortCallServiceTypeCode> getValidPortCallServiceTypeCodes(
      PortCallServiceEventTypeCode code) {
    return switch (code) {
      case STRT, CMPL ->
          Set.of(
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
          Set.of(
              BERTH,
              PILOT_BOARDING_PLACE,
              ALL_FAST,
              GANGWAY_DOWN_AND_SECURE,
              VESSEL_READY_FOR_CARGO_OPERATIONS,
              VESSEL_READY_TO_SAIL,
              MOVES);
      case DEPA -> Set.of(SAFETY, BERTH);
    };
  }

  public static boolean isValidCombination(
      PortCallServiceTypeCode portCallServiceTypeCode, String portCallServiceEventTypeCode) {
    PortCallServiceEventTypeCode code = fromString(portCallServiceEventTypeCode);
    return getValidPortCallServiceTypeCodes(code).contains(portCallServiceTypeCode);
  }

  public static List<PortCallServiceEventTypeCode> getCodesForPortCallServiceTypeCode(
      String portCallServiceTypeCode) {
    PortCallServiceTypeCode givenCode = PortCallServiceTypeCode.fromName(portCallServiceTypeCode);
    return Arrays.stream(PortCallServiceEventTypeCode.values())
        .filter(code -> getValidPortCallServiceTypeCodes(code).contains(givenCode))
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
