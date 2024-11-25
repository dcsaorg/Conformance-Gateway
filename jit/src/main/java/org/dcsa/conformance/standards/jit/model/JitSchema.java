package org.dcsa.conformance.standards.jit.model;

public enum JitSchema {
  PORT_CALL_SERVICE("PortCallService"),
  ESTIMATED_TIMESTAMP("EstimatedTimestamp"),
  PLANNED_TIMESTAMP("PlannedTimestamp"),
  REQUESTED_TIMESTAMP("RequestedTimestamp"),
  ACTUAL_TIMESTAMP("ActualTimestamp"),
  CANCEL("Cancel"),
  DECLINE("Decline");

  private final String schemaName;

  JitSchema(String schemaName) {
    this.schemaName = schemaName;
  }

  public String getSchemaName() {
    return schemaName;
  }
}
