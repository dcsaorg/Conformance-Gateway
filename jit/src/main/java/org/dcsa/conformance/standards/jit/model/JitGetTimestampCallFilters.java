package org.dcsa.conformance.standards.jit.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public record JitGetTimestampCallFilters(
    String timestampID_Estimated,
    String timestampID_Requested,
    String timestampID_Planned,
    String timestampID_Actual,
    String portCallServiceID,
    String classifierCode) {

  public static List<String> props() {
    return Arrays.stream(JitGetTimestampCallFilters.class.getDeclaredFields())
        .map(Field::getName)
        .toList();
  }
}
