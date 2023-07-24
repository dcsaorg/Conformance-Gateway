package org.dcsa.conformance.gateway.analysis;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.gateway.check.ConformanceCheck;

public class ConformanceReport {
  @Getter private final String title;
  @Getter private final ConformanceStatus conformanceStatus;
  @Getter private int conformantExchangeCount;
  @Getter private int nonConformantExchangeCount;
  @Getter private final List<ConformanceReport> subReports;

  public ConformanceReport(ConformanceCheck conformanceCheck, String roleName) {
    this.title = conformanceCheck.getTitle();
    conformanceCheck
        .resultsStream()
        .forEach(
            result -> {
              if (result.isConformant()) {
                ++conformantExchangeCount;
              } else {
                ++nonConformantExchangeCount;
              }
            });
    this.subReports =
        conformanceCheck
            .subChecksStream()
            .filter(check -> check.isRelevantForRole(roleName))
            .map(subCheck -> new ConformanceReport(subCheck, roleName))
            .collect(Collectors.toList());
    this.conformanceStatus =
        this.subReports.stream()
            .map(subReport -> subReport.conformanceStatus)
            .reduce(
                (conformanceStatus1, conformanceStatus2) -> {
                  if (ConformanceStatus.NON_CONFORMANT.equals(conformanceStatus1)
                      || ConformanceStatus.NON_CONFORMANT.equals(conformanceStatus2)) {
                    return ConformanceStatus.NON_CONFORMANT;
                  }
                  if (ConformanceStatus.PARTIALLY_CONFORMANT.equals(conformanceStatus1)
                      || ConformanceStatus.PARTIALLY_CONFORMANT.equals(conformanceStatus2)) {
                    return ConformanceStatus.PARTIALLY_CONFORMANT;
                  }
                  if (ConformanceStatus.CONFORMANT.equals(conformanceStatus1)
                      && ConformanceStatus.CONFORMANT.equals(conformanceStatus2)) {
                    return ConformanceStatus.CONFORMANT;
                  }
                  if (ConformanceStatus.NO_TRAFFIC.equals(conformanceStatus1)
                      && ConformanceStatus.NO_TRAFFIC.equals(conformanceStatus2)) {
                    return ConformanceStatus.NO_TRAFFIC;
                  }
                  return ConformanceStatus.PARTIALLY_CONFORMANT;
                })
            .orElse(
                ConformanceStatus.forExchangeCounts(
                    conformantExchangeCount, nonConformantExchangeCount));
  }

  public static Map<String, ConformanceReport> createForRoles(
      ConformanceCheck conformanceCheck, String... roleNames) {
    return Arrays.stream(roleNames)
        .collect(
            Collectors.toMap(
                roleName -> roleName,
                roleName -> new ConformanceReport(conformanceCheck, roleName),
                (k1, k2) -> k1,
                TreeMap::new));
  }
}
