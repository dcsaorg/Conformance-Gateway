package org.dcsa.conformance.standards.jit.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JitTimestampType {
  ESTIMATED(JitSchema.ESTIMATED_TIMESTAMP, JitClassifierCode.EST),
  PLANNED(JitSchema.PLANNED_TIMESTAMP, JitClassifierCode.PLN),
  ACTUAL(JitSchema.ACTUAL_TIMESTAMP, JitClassifierCode.ACT),
  REQUESTED(JitSchema.REQUESTED_TIMESTAMP, JitClassifierCode.REQ);

  private final JitSchema jitSchema;
  private final JitClassifierCode classifierCode;

  public static JitTimestampType fromClassifierCode(final JitClassifierCode classifierCode) {
    for (JitTimestampType jitTimestampType : values()) {
      if (jitTimestampType.getClassifierCode() == classifierCode) {
        return jitTimestampType;
      }
    }
    throw new IllegalArgumentException("Unknown classifier code: " + classifierCode.name());
  }
}
