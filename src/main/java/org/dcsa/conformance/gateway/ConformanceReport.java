package org.dcsa.conformance.gateway;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.Getter;

public class ConformanceReport {
  @Getter private final String title;
  @Getter private final List<ConformanceReport> subReports;

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

  public ConformanceReport(ConformanceCheck conformanceCheck, String roleName) {
    this.title = conformanceCheck.getTitle();
    this.subReports =
        conformanceCheck
            .getSubChecks()
            .filter(
                check -> {
                  boolean relevant = check.isRelevantForRole(roleName);
                  System.out.println("%s relevant for role '%s': %s".formatted(
                          relevant ? "IS" : "NOT",
                          roleName,
                          check.getTitle()
                  ));
                  return relevant;
                })
            .map(subCheck -> new ConformanceReport(subCheck, roleName))
            .collect(Collectors.toList());
  }
}
