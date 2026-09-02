package org.dcsa.conformance.core.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

/**
 * Covers the aggregation of a module whose scenarios are interchangeable alternatives, of which the
 * tested party only has to execute one.
 */
class ConformanceReportInterchangeableScenarioTest {

  private static final int ALTERNATIVE_COUNT = 5;

  @Test
  void moduleIsConformantWhenASingleAlternativeWasExecutedConformantly() {
    ConformanceReport report = reportForAlternatives(Outcome.CONFORMANT, Outcome.NOT_EXECUTED,
      Outcome.NOT_EXECUTED, Outcome.NOT_EXECUTED, Outcome.NOT_EXECUTED);

    assertEquals(ConformanceStatus.CONFORMANT, report.getConformanceStatus());
    assertEquals(
      ConformanceStatus.NO_TRAFFIC, report.getSubReports().get(1).getConformanceStatus());
  }

  @Test
  void moduleIsConformantWhenSeveralAlternativesWereExecutedConformantly() {
    ConformanceReport report = reportForAlternatives(Outcome.CONFORMANT, Outcome.NOT_EXECUTED,
      Outcome.CONFORMANT, Outcome.NOT_EXECUTED, Outcome.NOT_EXECUTED);

    assertEquals(ConformanceStatus.CONFORMANT, report.getConformanceStatus());
  }

  @Test
  void moduleHasNoTrafficWhenNoAlternativeWasExecuted() {
    ConformanceReport report = reportForAlternatives(Outcome.NOT_EXECUTED, Outcome.NOT_EXECUTED,
      Outcome.NOT_EXECUTED, Outcome.NOT_EXECUTED, Outcome.NOT_EXECUTED);

    assertEquals(ConformanceStatus.NO_TRAFFIC, report.getConformanceStatus());
  }

  @Test
  void moduleIsNonConformantWhenAnExecutedAlternativeFailed() {
    ConformanceReport report = reportForAlternatives(Outcome.CONFORMANT, Outcome.NON_CONFORMANT,
      Outcome.NOT_EXECUTED, Outcome.NOT_EXECUTED, Outcome.NOT_EXECUTED);

    assertEquals(ConformanceStatus.NON_CONFORMANT, report.getConformanceStatus());
  }

  @Test
  void requiredScenariosOfTheSameModuleStillAllHaveToBeExecuted() {
    ConformanceCheck moduleCheck =
      moduleCheck(
        scenarioCheck("Required", Outcome.NOT_EXECUTED, ScenarioConformanceType.REQUIRED),
        scenarioCheck("Alternative 1", Outcome.CONFORMANT, ScenarioConformanceType.INTERCHANGEABLE),
        scenarioCheck(
          "Alternative 2", Outcome.NOT_EXECUTED, ScenarioConformanceType.INTERCHANGEABLE));

    assertEquals(
      ConformanceStatus.PARTIALLY_CONFORMANT, reportFor(moduleCheck).getConformanceStatus());
  }

  @Test
  void optionalScenariosOfTheSameModuleAreStillIgnored() {
    ConformanceCheck moduleCheck =
      moduleCheck(
        scenarioCheck("Optional", Outcome.NON_CONFORMANT, ScenarioConformanceType.OPTIONAL),
        scenarioCheck("Alternative 1", Outcome.CONFORMANT, ScenarioConformanceType.INTERCHANGEABLE),
        scenarioCheck(
          "Alternative 2", Outcome.NOT_EXECUTED, ScenarioConformanceType.INTERCHANGEABLE));

    assertEquals(ConformanceStatus.CONFORMANT, reportFor(moduleCheck).getConformanceStatus());
  }

  private static ConformanceReport reportForAlternatives(Outcome... outcomes) {
    assertEquals(ALTERNATIVE_COUNT, outcomes.length);
    ConformanceCheck[] scenarioChecks =
      java.util.stream.IntStream.range(0, outcomes.length)
        .mapToObj(
          index ->
            scenarioCheck(
              "Alternative " + index,
              outcomes[index],
              ScenarioConformanceType.INTERCHANGEABLE))
        .toArray(ConformanceCheck[]::new);
    return reportFor(moduleCheck(scenarioChecks));
  }

  private static ConformanceReport reportFor(ConformanceCheck moduleCheck) {
    moduleCheck.check(ignored -> null);
    return new ConformanceReport(moduleCheck, "AnyRole");
  }

  private static ConformanceCheck moduleCheck(ConformanceCheck... scenarioChecks) {
    return new ConformanceCheck("Module") {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Arrays.stream(scenarioChecks);
      }
    };
  }

  private static ConformanceCheck scenarioCheck(
    String title, Outcome outcome, ScenarioConformanceType conformanceType) {
    return new ScenarioCheck(
      new ConformanceScenario(
        UUID.randomUUID(), List.of(new StubAction(title, outcome)), conformanceType),
      "1.0.0");
  }

  private enum Outcome {
    CONFORMANT,
    NON_CONFORMANT,
    NOT_EXECUTED
  }

  private static final class StubAction extends ConformanceAction {
    private final Outcome outcome;

    private StubAction(String title, Outcome outcome) {
      super("source", "target", null, title);
      this.outcome = outcome;
    }

    @Override
    public String getHumanReadablePrompt() {
      return "";
    }

    @Override
    public ConformanceCheck createCheck(String expectedApiVersion) {
      return new ConformanceCheck(getActionTitle()) {
        @Override
        protected void doCheck(Function<UUID, ConformanceExchange> ignored) {
          // A scenario that was never executed produces no result at all, which the framework
          // reports as NO_TRAFFIC.
          switch (outcome) {
            case CONFORMANT -> addResult(ConformanceResult.withErrors(Set.of()));
            case NON_CONFORMANT ->
              addResult(ConformanceResult.withErrors(Set.of("Alternative failed")));
            case NOT_EXECUTED -> {
              // no traffic
            }
          }
        }
      };
    }
  }
}

