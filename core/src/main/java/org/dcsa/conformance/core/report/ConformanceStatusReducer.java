package org.dcsa.conformance.core.report;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.List;

@UtilityClass
public class ConformanceStatusReducer {

  public static ConformanceStatus reduce(
    ConformanceStatus conformanceStatus1, ConformanceStatus conformanceStatus2) {
    List<ConformanceStatus> statuses = List.of(conformanceStatus1.forAggregation(), conformanceStatus2.forAggregation());

    if (statuses.contains(ConformanceStatus.NON_CONFORMANT)) {
      return ConformanceStatus.NON_CONFORMANT;
    }
    if (statuses.contains(ConformanceStatus.PARTIALLY_CONFORMANT)) {
      return ConformanceStatus.PARTIALLY_CONFORMANT;
    }
    if (statuses.stream()
      .allMatch(status ->
        status == ConformanceStatus.CONFORMANT || status == ConformanceStatus.IRRELEVANT)) {
      return ConformanceStatus.CONFORMANT;
    }
    if (statuses.stream()
      .allMatch(
        status ->
          status == ConformanceStatus.NO_TRAFFIC
            || status == ConformanceStatus.IRRELEVANT)) {
      return ConformanceStatus.NO_TRAFFIC;
    }
    return ConformanceStatus.PARTIALLY_CONFORMANT;
  }

  public static ConformanceStatus reduceInterchangeable(Collection<ConformanceStatus> statuses) {
    if (statuses.isEmpty()) {
      return ConformanceStatus.IRRELEVANT;
    }
    return statuses.stream()
      .map(ConformanceStatus::forAggregation)
      .filter(ConformanceStatusReducer::wasExecuted)
      .reduce(ConformanceStatusReducer::reduce)
      .orElse(ConformanceStatus.NO_TRAFFIC);
  }

  private static boolean wasExecuted(ConformanceStatus aggregationStatus) {
    return aggregationStatus != ConformanceStatus.NO_TRAFFIC
      && aggregationStatus != ConformanceStatus.IRRELEVANT;
  }
}
