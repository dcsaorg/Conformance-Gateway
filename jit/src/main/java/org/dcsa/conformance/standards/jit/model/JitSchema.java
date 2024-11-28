package org.dcsa.conformance.standards.jit.model;

import lombok.Getter;

@Getter
public enum JitSchema {
  PORT_CALL_SERVICE("PortCallService"),
  ESTIMATED_TIMESTAMP(Constants.TIMESTAMP),
  PLANNED_TIMESTAMP(Constants.TIMESTAMP),
  REQUESTED_TIMESTAMP(Constants.TIMESTAMP),
  ACTUAL_TIMESTAMP(Constants.TIMESTAMP),
  CANCEL("Cancel"),
  DECLINE("Decline");

  private final String schemaName;

  JitSchema(String schemaName) {
    this.schemaName = schemaName;
  }

  private static class Constants {
    public static final String TIMESTAMP = "Timestamp";
  }
}
