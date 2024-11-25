package org.dcsa.conformance.standards.jit.model;

import lombok.Getter;

public enum JitTimestampType {
  ESTIMATED(JitSchema.ESTIMATED_TIMESTAMP),
  PLANNED(JitSchema.PLANNED_TIMESTAMP),
  ACTUAL(JitSchema.ACTUAL_TIMESTAMP),
  REQUESTED(JitSchema.REQUESTED_TIMESTAMP);

  @Getter private final JitSchema jitSchema;

  JitTimestampType(JitSchema jitSchema) {
    this.jitSchema = jitSchema;
  }
}
