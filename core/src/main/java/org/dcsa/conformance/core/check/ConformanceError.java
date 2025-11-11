package org.dcsa.conformance.core.check;

public record ConformanceError(String message, ConformanceErrorSeverity severity) {

  private static final String IRRELEVANT_CHECK_FOR_ELEMENT_IN_POSITION =
      "Irrelevant check for element in the position %d";
  private static final String IRRELEVANT_MESSAGE = "Irrelevant check";

  public static ConformanceError of(String message, ConformanceErrorSeverity severity) {
    return new ConformanceError(message, severity);
  }

  public static ConformanceError error(String message) {
    return new ConformanceError(message, ConformanceErrorSeverity.ERROR);
  }

  public static ConformanceError irrelevant() {
    return new ConformanceError(IRRELEVANT_MESSAGE, ConformanceErrorSeverity.IRRELEVANT);
  }

  public static ConformanceError irrelevant(int position) {
    return new ConformanceError(
        String.format(IRRELEVANT_CHECK_FOR_ELEMENT_IN_POSITION, position),
        ConformanceErrorSeverity.IRRELEVANT);
  }

  public boolean isConformant() {
    return severity.isConformant();
  }
}
