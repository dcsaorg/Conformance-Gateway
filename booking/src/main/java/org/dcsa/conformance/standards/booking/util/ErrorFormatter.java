package org.dcsa.conformance.standards.booking.util;

import java.util.Collection;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ErrorFormatter {

  public static String formatErrorsForUserFacingException(Collection<String> errors) {
    return "The booking input has the following errors:\n\n"
        + errors.stream()
            .map(error -> " 🚫 " + error.replace(": ", ""))
            .collect(Collectors.joining("\n"));
  }
}
