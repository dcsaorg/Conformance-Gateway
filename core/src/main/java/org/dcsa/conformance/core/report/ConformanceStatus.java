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

    boolean hasConformantExchanges = conformantExchangeCount > 0;
    boolean hasNonConformantExchanges = nonConformantExchangeCount > 0;

    if (!hasConformantExchanges && !hasNonConformantExchanges) {
      return NO_TRAFFIC;
    }

    if (hasConformantExchanges && !hasNonConformantExchanges) {
      return CONFORMANT;
    }

    if (!hasConformantExchanges) {
      return NON_CONFORMANT;
    }

    return PARTIALLY_CONFORMANT;
  }
}
