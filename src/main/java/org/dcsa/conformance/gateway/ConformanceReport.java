package org.dcsa.conformance.gateway;

public class ConformanceReport {

  public ConformanceReport(ConformanceCheck conformanceCheck) {
  }

  public String toString() {
    return "{\"exchangeCount\": %d}".formatted(6);
  }
}
