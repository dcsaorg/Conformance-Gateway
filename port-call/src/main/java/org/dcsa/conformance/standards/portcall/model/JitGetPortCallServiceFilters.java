package org.dcsa.conformance.standards.portcall.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public record JitGetPortCallServiceFilters(
    String terminalCallID, String portCallServiceID, String portCallServiceTypeCode) {

  public static List<String> props() {
    return Arrays.stream(JitGetPortCallServiceFilters.class.getDeclaredFields())
        .map(Field::getName)
        .toList();
  }
}
