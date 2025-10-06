package org.dcsa.conformance.core.report;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConformanceStatusReducer {

  public static ConformanceStatus reduce(
      ConformanceStatus conformanceStatus1, ConformanceStatus conformanceStatus2) {
    List<ConformanceStatus> statuses = List.of(conformanceStatus1, conformanceStatus2);

    if (statuses.contains(ConformanceStatus.NON_CONFORMANT)) {
      return ConformanceStatus.NON_CONFORMANT;
    }
    if (statuses.contains(ConformanceStatus.PARTIALLY_CONFORMANT)) {
      return ConformanceStatus.PARTIALLY_CONFORMANT;
    }
    if (statuses.stream()
        .allMatch(
            status ->
                status == ConformanceStatus.CONFORMANT
                    || status == ConformanceStatus.NOT_RELEVANT)) {
      return ConformanceStatus.CONFORMANT;
    }
    if (statuses.stream().allMatch(ConformanceStatus.NO_TRAFFIC::equals)) {
      return ConformanceStatus.NO_TRAFFIC;
    }
    return ConformanceStatus.PARTIALLY_CONFORMANT;
  }
}
