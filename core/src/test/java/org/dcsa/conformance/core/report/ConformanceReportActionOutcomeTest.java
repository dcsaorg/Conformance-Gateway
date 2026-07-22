package org.dcsa.conformance.core.report;

import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceResult;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConformanceReportActionOutcomeTest {

  @Test
  void explicitSkipPropagatesToChecksAndIsAcceptedByScenario() {
    ConformanceCheck skippedAction = container("GET", noTraffic("HTTP check"))
      .withStatusOverride(ConformanceStatus.SKIPPED, true);
    ConformanceReport report = report(container("Scenario", skippedAction, conformant("Next action")));

    assertEquals(ConformanceStatus.CONFORMANT, report.getConformanceStatus());
    assertEquals(ConformanceStatus.SKIPPED, report.getSubReports().get(0).getConformanceStatus());
    assertEquals(
      ConformanceStatus.SKIPPED,
      report.getSubReports().get(0).getSubReports().get(0).getConformanceStatus());
  }

  @Test
  void completionWithoutTrafficDoesNotHideMissingNotificationChecks() {
    ConformanceCheck carrierAction = container("UC5", noTraffic("Notification check"))
      .withStatusOverride(ConformanceStatus.COMPLETED_WITHOUT_TRAFFIC, false);
    ConformanceReport report = report(container("Scenario", carrierAction, conformant("GET")));

    assertEquals(ConformanceStatus.CONFORMANT, report.getConformanceStatus());
    assertEquals(
      ConformanceStatus.COMPLETED_WITHOUT_TRAFFIC,
      report.getSubReports().get(0).getConformanceStatus());
    assertEquals(
      ConformanceStatus.NO_TRAFFIC,
      report.getSubReports().get(0).getSubReports().get(0).getConformanceStatus());
  }

  @Test
  void invalidDownstreamGetStillMakesScenarioNonConformant() {
    ConformanceCheck carrierAction = container("UC5", noTraffic("Notification check"))
      .withStatusOverride(ConformanceStatus.COMPLETED_WITHOUT_TRAFFIC, false);
    ConformanceReport report = report(container("Scenario", carrierAction, nonConformant("GET")));

    assertEquals(ConformanceStatus.NON_CONFORMANT, report.getConformanceStatus());
  }

  private static ConformanceReport report(ConformanceCheck check) {
    check.check(ignored -> null);
    return new ConformanceReport(check, "Carrier");
  }

  private static ConformanceCheck container(String title, ConformanceCheck... children) {
    return new ConformanceCheck(title) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(children);
      }
    };
  }

  private static ConformanceCheck noTraffic(String title) {
    return new ConformanceCheck(title) {};
  }

  private static ConformanceCheck conformant(String title) {
    return result(title, Set.of());
  }

  private static ConformanceCheck nonConformant(String title) {
    return result(title, Set.of("Wrong booking status"));
  }

  private static ConformanceCheck result(String title, Set<String> errors) {
    return new ConformanceCheck(title) {
      @Override
      protected void doCheck(Function<java.util.UUID, org.dcsa.conformance.core.traffic.ConformanceExchange> ignored) {
        addResult(ConformanceResult.withErrors(errors));
      }
    };
  }
}


