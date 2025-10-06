package org.dcsa.conformance.core.report;

public enum ConformanceStatus {
  NO_TRAFFIC,
  NON_CONFORMANT,
  PARTIALLY_CONFORMANT,
  CONFORMANT,
  NOT_RELEVANT;

  public static ConformanceStatus forExchangeCounts(
      int conformantExchangeCount, int nonConformantExchangeCount, boolean isRelevant) {
    if (!isRelevant) {
      return NOT_RELEVANT;
    }
    return conformantExchangeCount == 0
        ? (nonConformantExchangeCount == 0 ? NO_TRAFFIC : NON_CONFORMANT)
        : (nonConformantExchangeCount == 0 ? CONFORMANT : PARTIALLY_CONFORMANT);
  }
}
