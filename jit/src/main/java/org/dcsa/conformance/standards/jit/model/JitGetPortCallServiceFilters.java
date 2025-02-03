package org.dcsa.conformance.standards.jit.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public record JitGetPortCallServiceFilters(
    String terminalCallID, String portCallServiceID, String portCallServiceType) {

  public static List<String> props() {
    return Arrays.stream(JitGetPortCallServiceFilters.class.getDeclaredFields())
        .map(Field::getName)
        .toList();
  }
}
