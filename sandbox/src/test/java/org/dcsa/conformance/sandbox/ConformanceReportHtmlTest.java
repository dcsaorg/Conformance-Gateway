package org.dcsa.conformance.sandbox;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceResult;
import org.dcsa.conformance.core.report.ConformanceReport;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.junit.jupiter.api.Test;

class ConformanceReportHtmlTest {

  @Test
  void declaresUtf8BeforeRenderingReportSymbols() {
    ConformanceCheck check =
        new ConformanceCheck("Scenario") {
          @Override
          protected void doCheck(Function<java.util.UUID, ConformanceExchange> ignored) {
            addResult(ConformanceResult.withErrors(Set.of()));
          }
        };
    check.check(ignored -> null);
    ConformanceReport report = new ConformanceReport(check, "Carrier");

    String html = ConformanceReport.toHtmlReport(Map.of("Carrier", report), false);
    String browserDecodedHtml = new String(html.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

    assertTrue(html.contains("<meta charset=\"utf-8\">"));
    assertTrue(html.indexOf("<meta charset=\"utf-8\">") < html.indexOf("<body"));
    assertTrue(browserDecodedHtml.contains("✅ CONFORMANT"));
  }
}

