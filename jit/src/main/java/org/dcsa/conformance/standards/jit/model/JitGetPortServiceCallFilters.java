package org.dcsa.conformance.standards.jit.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public record JitGetPortServiceCallFilters(
    String terminalCallID, String portCallServiceID, String portCallServiceType) {

  public static List<String> props() {
    return Arrays.stream(JitGetPortServiceCallFilters.class.getDeclaredFields())
        .map(Field::getName)
        .toList();
  }
}
