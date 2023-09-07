package org.dcsa.conformance.gateway.analysis;

public enum ConformanceStatus {
  NO_TRAFFIC,
  NON_CONFORMANT,
  PARTIALLY_CONFORMANT,
  CONFORMANT;

  public static ConformanceStatus forExchangeCounts(
      int conformantExchangeCount, int nonConformantExchangeCount) {
    return conformantExchangeCount == 0
        ? (nonConformantExchangeCount == 0 ? NO_TRAFFIC : NON_CONFORMANT)
        : (nonConformantExchangeCount == 0 ? CONFORMANT : PARTIALLY_CONFORMANT);
  }
}
