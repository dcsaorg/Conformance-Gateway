package org.dcsa.conformance.standards.jit.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public record JitGetPortCallFilters(
    String portCallID,
    String portVisitReference,
    String UNLocationCode,
    String vesselIMONumber,
    String vesselName,
    String MMSINumber) {

  public static List<String> props() {
    return Arrays.stream(JitGetPortCallFilters.class.getDeclaredFields())
        .map(Field::getName)
        .toList();
  }
}
