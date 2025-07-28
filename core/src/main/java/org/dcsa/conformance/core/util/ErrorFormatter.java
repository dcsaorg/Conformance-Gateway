package org.dcsa.conformance.core.util;

import java.util.Collection;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ErrorFormatter {

  public static String formatInputErrors(Collection<String> errors) {
    return "The input has the following errors:\n\n"
        + errors.stream()
            .map(error -> " 🚫 " + error.replace(": ", ""))
            .collect(Collectors.joining("\n"));
  }
}
