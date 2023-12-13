package org.dcsa.conformance.core.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.SneakyThrows;
import org.dcsa.conformance.core.check.ConformanceCheck;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class ConformanceReport {
  private final ConformanceCheck conformanceCheck;
  private final String title;
  private final ConformanceStatus conformanceStatus;
  private int conformantExchangeCount;
  private int nonConformantExchangeCount;
  private final Set<String> errorMessages = new LinkedHashSet<>();
  private final List<ConformanceReport> subReports;

  public ConformanceReport(ConformanceCheck conformanceCheck, String roleName) {
    this.conformanceCheck = conformanceCheck;
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
    conformanceCheck.computedStatusConsumer().accept(this.conformanceStatus);
  }

  public static Map<String, ConformanceReport> createForRoles(
      ConformanceCheck conformanceCheck, Set<String> roleNames) {
    return roleNames.stream()
        .collect(
            Collectors.toMap(
                roleName -> roleName,
                roleName -> new ConformanceReport(conformanceCheck, roleName),
                (k1, k2) -> k1,
                TreeMap::new));
  }

  public JsonNode toJsonReport() {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode reportNode = objectMapper.createObjectNode();

    reportNode.put("title", title);
    reportNode.put("status", conformanceStatus.name());

    ArrayNode subReportsNode = objectMapper.createArrayNode();
    subReports.forEach(subReport -> subReportsNode.add(subReport.toJsonReport()));
    reportNode.set("subReports", subReportsNode);

    ArrayNode errorsNode = objectMapper.createArrayNode();
    errorMessages.forEach(errorsNode::add);
    reportNode.set("errorMessages", errorsNode);

    return reportNode;
  }

  public static String toHtmlReport(
      Map<String, ConformanceReport> reportsByRole, boolean printable) {
    return String.join(
        "\n",
        "<!DOCTYPE html>",
        "<html>",
        "<body style=\"font-family: sans-serif;\">",
        "<div>%s</div>".formatted(getDcsaLogoImage()),
        "<h1>Conformance Report</h1>",
        reportsByRole.entrySet().stream()
            .map(
                roleAndReport ->
                    renderReportForRole(
                        roleAndReport.getKey(), roleAndReport.getValue(), printable))
            .collect(Collectors.joining("\n")),
        "</body>",
        "</html>");
  }

  private static String renderReportForRole(
      String role, ConformanceReport report, boolean printable) {
    return "<h2>%s conformance</h2><details open><summary>%s %s </summary>%n%s%n</details>%n"
        .formatted(
            role,
            getConformanceIcon(report.conformanceStatus),
            getConformanceLabel(report.conformanceStatus),
            renderReport(report, 0, printable));
  }

  private static String renderReport(ConformanceReport report, int level, boolean printable) {
    if (level == 0) {
      return scenarioListAsHtmlBlock(report, level, printable);
    }
    if (level == 1) {
      return scenarioAsHtmlBlock(report, level, printable);
    }
    if (level > 1) {
      return scenarioDetailsAsHtmlBlock(report, level, printable);
    }
    return asHtmlBlock(report, level, printable);
  }

  private static String scenarioAsHtmlBlock(ConformanceReport report, int level, boolean printable) {
    return "<div style=\"margin-left: %dem\">%n<details%s><summary>%s %s</summary>%n<div>%s</div>%n%s%n</details></div>%n"
        .formatted(
            level * 2,
            printable ? " open" : "",
            getConformanceIcon(report.conformanceStatus),
            report.title,
            getErrors(report),
            report.subReports.stream()
                .map(subReport -> renderReport(subReport, level + 1, printable))
                .collect(Collectors.joining("\n")));
  }

  private static String scenarioDetailsAsHtmlBlock(ConformanceReport report, int level, boolean printable) {
    if (report.subReports.isEmpty() && report.errorMessages.isEmpty()) {
      return "<h5 style=\"margin-left: %dem\">%s %s (%s)</h5>"
          .formatted(
              level * 2,
              getConformanceIcon(report.conformanceStatus),
              report.title.trim(),
              getConformanceLabel(report.conformanceStatus));
    }
    return "<div style=\"margin-left: %dem\">%n<details%s><summary>%s %s</summary>%n<div>%s</div>%n%s%n</details></div>%n"
        .formatted(
            level * 2,
            printable ? " open" : "",
            getConformanceIcon(report.conformanceStatus),
            report.title,
            getErrors(report),
            report.subReports.stream()
                .map(subReport -> renderReport(subReport, level + 1, printable))
                .collect(Collectors.joining("\n")));
  }

  private static String scenarioListAsHtmlBlock(ConformanceReport report, int level, boolean printable) {
    return "<div style=\"margin-left: %dem\">%n<h4>%s</h4>%n<div>%s</div>%n</div>%n%s%n"
        .formatted(
            level * 2,
            report.title,
            getErrors(report),
            report.subReports.stream()
                .map(subReport -> renderReport(subReport, level + 1, printable))
                .collect(Collectors.joining("\n")));
  }

  private static String asHtmlBlock(ConformanceReport report, int level, boolean printable) {
    return "<div style=\"margin-left: %dem\">%n<h4>%s</h4>%n<div>%s %s %s</div>%n<div>%s</div>%n</div>%n%s%n"
        .formatted(
            level * 2,
            report.title,
            getConformanceIcon(report.conformanceStatus),
            getConformanceLabel(report.conformanceStatus),
            getExchangesDetails(report),
            getErrors(report),
            report.subReports.stream()
                .map(subReport -> renderReport(subReport, level + 1, printable))
                .collect(Collectors.joining("\n")));
  }

  private static String getConformanceIcon(ConformanceStatus conformanceStatus) {
    return switch (conformanceStatus) {
      case CONFORMANT -> "✅";
      case PARTIALLY_CONFORMANT -> "⚠️";
      case NON_CONFORMANT -> "🚫";
      default -> "❔";
    };
  }

  private static String getConformanceLabel(ConformanceStatus conformanceStatus) {
    return switch (conformanceStatus) {
      case CONFORMANT -> "CONFORMANT";
      case PARTIALLY_CONFORMANT -> "PARTIALLY CONFORMANT";
      case NON_CONFORMANT -> "NON-CONFORMANT";
      default -> "NO TRAFFIC";
    };
  }

  private static String getExchangesDetails(ConformanceReport report) {
    if (Instant.now().toEpochMilli() > 0) return "";
    if (report.conformanceStatus.equals(ConformanceStatus.NO_TRAFFIC)) return "";
    if (!report.subReports.isEmpty()) return "";
    return "%n<ul>%s%s</ul>"
        .formatted(
            report.conformantExchangeCount == 0
                ? ""
                : "<li>%d conformant exchange%s</li>"
                    .formatted(
                        report.conformantExchangeCount,
                        report.conformantExchangeCount == 1 ? "" : "s"),
            report.nonConformantExchangeCount == 0
                ? ""
                : "<li>%d non-conformant exchange%s</li>"
                    .formatted(
                        report.nonConformantExchangeCount,
                        report.nonConformantExchangeCount == 1 ? "" : "s"));
  }

  private static String getErrors(ConformanceReport report) {
    return report.errorMessages.stream()
        .map("\n<div>%s</div>"::formatted)
        .collect(Collectors.joining());
  }

  @SneakyThrows
  private static String getDcsaLogoImage() {
    try (InputStream logoStream =
        ConformanceReport.class.getResourceAsStream("/dcsa-logo-base64.txt")) {
      return "<img src=\"data:image/png;base64,%s\" alt=\"DCSA logo\"/>"
          .formatted(
              new String(Objects.requireNonNull(logoStream).readAllBytes(), StandardCharsets.UTF_8)
                  .replaceAll("\\s++", ""));
    }
  }
}
