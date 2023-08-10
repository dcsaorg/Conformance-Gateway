package org.dcsa.conformance.gateway.analysis;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.SneakyThrows;
import org.dcsa.conformance.gateway.check.ConformanceCheck;

public class ConformanceReport {
  @Getter private final String title;
  @Getter private final ConformanceStatus conformanceStatus;
  @Getter private int conformantExchangeCount;
  @Getter private int nonConformantExchangeCount;
  @Getter private final Set<String> errorMessages = new TreeSet<>();
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
              errorMessages.addAll(result.getErrors());
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

  public static String toHtmlReport(Map<String, ConformanceReport> reportsByRole) {
    return Stream.of(
            "<html>",
            "<body style=\"font-family: sans-serif;\">",
            "<div>%s</div>".formatted(getDcsaLogoImage()),
            "<h1>Conformance Report</h1>",
            reportsByRole.entrySet().stream()
                .map(
                    roleAndReport ->
                        "<h2>%s conformance</h2>\n%s\n"
                            .formatted(
                                roleAndReport.getKey(), asHtmlBlock(roleAndReport.getValue(), 0)))
                .collect(Collectors.joining("\n")),
            "</body>",
            "</html>")
        .collect(Collectors.joining("\n"));
  }

  private static String asHtmlBlock(ConformanceReport report, int indent) {
    return "<div style=\"margin-left: %dem\">\n<h4>%s</h4>\n<div>%s %s %s</div>\n<div>%s</div>\n</div>\n%s\n"
        .formatted(
            indent,
            report.title,
            getConformanceIcon(report.conformanceStatus),
            getConformanceLabel(report.conformanceStatus),
            getExchangesDetails(report),
            getErrors(report),
            report.subReports.stream()
                .map(subReport -> asHtmlBlock(subReport, indent + 2))
                .collect(Collectors.joining("\n")));
  }

  private static String getConformanceIcon(ConformanceStatus conformanceStatus) {
    switch (conformanceStatus) {
      case CONFORMANT:
        return "✅";
      case PARTIALLY_CONFORMANT:
        return "⚠️";
      case NON_CONFORMANT:
        return "🚫";
      case NO_TRAFFIC:
      default:
        return "❔";
    }
  }

  private static String getConformanceLabel(ConformanceStatus conformanceStatus) {
    switch (conformanceStatus) {
      case CONFORMANT:
        return "CONFORMANT";
      case PARTIALLY_CONFORMANT:
        return "PARTIALLY CONFORMANT";
      case NON_CONFORMANT:
        return "NON-CONFORMANT";
      case NO_TRAFFIC:
      default:
        return "NO TRAFFIC";
    }
  }

  private static String getExchangesDetails(ConformanceReport report) {
    if (report.conformanceStatus.equals(ConformanceStatus.NO_TRAFFIC)) return "";
    if (!report.subReports.isEmpty()) return "";
    return "\n<ul><li>%d conformant exchanges</li><li>%d non-conformant exchanges</li></ul>"
        .formatted(report.conformantExchangeCount, report.nonConformantExchangeCount);
  }

  private static String getErrors(ConformanceReport report) {
    return report.errorMessages.stream()
        .map(message -> "\n<div>%s</div>".formatted(message))
        .collect(Collectors.joining());
  }

  @SneakyThrows
  private static String getDcsaLogoImage() {
    return "<img src=\"data:image/png;base64,\n%s\n\" alt=\"DCSA logo\"/>"
        .formatted(
            new String(
                ConformanceReport.class.getResourceAsStream("/dcsa-logo-base64.txt").readAllBytes(),
                StandardCharsets.UTF_8));
  }
}
