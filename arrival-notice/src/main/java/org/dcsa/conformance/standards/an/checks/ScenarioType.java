package org.dcsa.conformance.standards.an.checks;

public enum ScenarioType {
  REGULAR,
  FREIGHTED,
  FREE_TIME;

  public String arrivalNoticePayload(String version) {
    String suffix =
        switch (this) {
          case FREIGHTED -> "freighted";
          case FREE_TIME -> "freetime";
          default -> "regular";
        };
    return "arrivalnotice-api-" + version + "-post-" + suffix + "-request.json";
  }
}
