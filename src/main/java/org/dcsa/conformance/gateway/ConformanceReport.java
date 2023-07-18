package org.dcsa.conformance.gateway;

import java.util.List;
import java.util.stream.Collectors;

public class ConformanceReport {
  private final int exchangeCount;
  private final ConformanceCode conformanceCode;

  private final List<ConformanceReport> subReports;

  public ConformanceReport(ConformanceCheck conformanceCheck) {
    this.exchangeCount = conformanceCheck.getExchangeCount();
    this.conformanceCode = conformanceCheck.getConformanceCode();
    this.subReports = conformanceCheck.getSubChecks().stream()
            .map(ConformanceReport::new)
            .collect(Collectors.toList());
  }

  public String toString() {
    return "{\"exchangeCount\": %d}".formatted(exchangeCount);
  }
}
