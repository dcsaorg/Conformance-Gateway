package org.dcsa.conformance.standards.jit.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JitSchema {
  PORT_CALL("PortCall"),
  TERMINAL_CALL("TerminalCall"),
  PORT_CALL_SERVICE("PortCallService"),
  VESSEL("Vessel"),
  ESTIMATED_TIMESTAMP(Constants.TIMESTAMP),
  PLANNED_TIMESTAMP(Constants.TIMESTAMP),
  REQUESTED_TIMESTAMP(Constants.TIMESTAMP),
  ACTUAL_TIMESTAMP(Constants.TIMESTAMP),
  CANCEL("Cancel"),
  DECLINE("Decline"),
  MOVES("Moves"),
  OMIT_PORT_CALL("OmitPortCall"),
  OMIT_TERMINAL_CALL("OmitTerminalCall"),
  // GET actions
  PORT_CALLS("PortCalls"),
  TERMINAL_CALLS("TerminalCalls"),
  PORT_CALL_SERVICES("PortCallServices"),
  TIMESTAMPS("Timestamps"),
  VESSEL_STATUSES("VesselStatuses"),
  ;

  private final String schemaName;

  private static class Constants {
    public static final String TIMESTAMP = "Timestamp";
  }
}
