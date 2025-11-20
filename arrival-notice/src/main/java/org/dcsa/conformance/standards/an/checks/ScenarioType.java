package org.dcsa.conformance.standards.an.checks;

public enum ScenarioType {
  BASIC,
  FREIGHTED,
  FREE_TIME;

  public String arrivalNoticePayload(String version) {
    String suffix =
        switch (this) {
          case FREIGHTED -> "freighted";
          case FREE_TIME -> "freetime";
          default -> "basic";
        };
    return "arrivalnotice-api-" + version + "-post-" + suffix + "-request.json";
  }

  public String arrivalNoticeResponse(String version) {
    String suffix =
        switch (this) {
          case FREIGHTED -> "freighted";
          case FREE_TIME -> "freetime";
          default -> "basic";
        };
    return "arrivalnotice-api-" + version + "-get-" + suffix + "-response.json";
  }
}
