package org.dcsa.conformance.standards.portcall.party;

public enum ScenarioType {
  TIMESTAMP,
  MOVE_FORECAST;

  public String getLabel() {
    return switch (this) {
      case TIMESTAMP -> "timestamp";
      case MOVE_FORECAST -> "move forecasts";
    };
  }
}
