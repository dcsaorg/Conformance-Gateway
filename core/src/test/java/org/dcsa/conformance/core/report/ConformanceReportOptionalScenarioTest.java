package org.dcsa.conformance.core.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceResult;
import org.dcsa.conformance.core.check.ScenarioCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioConformanceType;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.junit.jupiter.api.Test;

class ConformanceReportOptionalScenarioTest {

  @Test
  void optionalScenarioDoesNotAffectOverallConformanceWhenItFails() {
    ConformanceCheck requiredScenarioCheck =
        new ScenarioCheck(
            new ConformanceScenario(
                1,
                1,
                List.of(new StubAction("Required action", Set.of())),
                ScenarioConformanceType.REQUIRED),
            "1.0.0");
    ConformanceCheck optionalScenarioCheck =
        new ScenarioCheck(
            new ConformanceScenario(
                1,
                2,
                List.of(new StubAction("Optional action", Set.of("Optional scenario failure"))),
                ScenarioConformanceType.OPTIONAL),
            "1.0.0");

    ConformanceCheck rootCheck =
        new ConformanceCheck("Module") {
          @Override
          protected Stream<? extends ConformanceCheck> createSubChecks() {
            return Stream.of(requiredScenarioCheck, optionalScenarioCheck);
          }
        };

    rootCheck.check(ignored -> null);
    ConformanceReport report = new ConformanceReport(rootCheck, "Carrier");

    assertEquals(ConformanceStatus.CONFORMANT, report.getConformanceStatus());
    assertEquals(ConformanceStatus.CONFORMANT, report.getSubReports().get(0).getConformanceStatus());
    assertEquals(
        ConformanceStatus.NON_CONFORMANT, report.getSubReports().get(1).getConformanceStatus());
  }

  private static final class StubAction extends ConformanceAction {
    private final Set<String> errors;

    private StubAction(String title, Set<String> errors) {
      super("source", "target", null, title);
      this.errors = errors;
    }

    @Override
    public String getHumanReadablePrompt() {
      return "";
    }

    @Override
    public ConformanceCheck createCheck(String expectedApiVersion) {
      return new ConformanceCheck(getActionTitle()) {
        @Override
        protected void doCheck(Function<java.util.UUID, ConformanceExchange> ignored) {
          addResult(ConformanceResult.withErrors(errors));
        }
      };
    }
  }
}
