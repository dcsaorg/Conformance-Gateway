package org.dcsa.conformance.standards.jit.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public record JitGetTerminalCallFilters(
    String portCallID,
    String terminalCallID,
    String carrierServiceName,
    String carrierServiceCode,
    String universalServiceReference,
    String terminalCallReference,
    String carrierImportVoyageNumber,
    String carrierExportVoyageNumber,
    String universalImportVoyageReference,
    String universalExportVoyageReference) {

  public static List<String> props() {
    return Arrays.stream(JitGetTerminalCallFilters.class.getDeclaredFields())
        .map(Field::getName)
        .toList();
  }
}
