package org.dcsa.conformance.manual;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.springboot.ConformanceApplication;
import org.dcsa.conformance.standards.ebl.EblScenarioListBuilder;
import org.dcsa.conformance.standards.ebl.EblStandard;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = ConformanceApplication.class)
class ManualScenarioWithNotificationsTest extends ManualTestBase {

  @SuppressWarnings("unused")
  private static Stream<String> testStandards() {
    return Stream.of(
        "Booking");
  }

  @ParameterizedTest(name = "Standard: {0}")
  @MethodSource("testStandards")
  void testStandards(String standardName) {
    app.setSimulatedLambdaDelay(lambdaDelay);
    getAllSandboxes();
    List<Standard> availableStandards = getAvailableStandards();
    Standard testingStandard =
        availableStandards.stream()
            .filter(standard -> standard.name().equals(standardName))
            .findFirst()
            .orElseThrow();

    testingStandard
        .versions()
        .forEach(
            version ->
                version.suites().stream()
                    .filter(suite -> suite.startsWith("Conformance"))
                    .forEach(
                        suite ->
                            version
                                .roles()
                                .forEach(
                                    role ->
                                        runManualTests(
                                            testingStandard.name(),
                                            version.number(),
                                            suite,
                                            role.name()))));
  }

  @Test
  @Disabled("Only for debugging")
  void testOnlyOneSpecificScenario() {
    runManualTests(
        EblStandard.INSTANCE.getName(),
        EblStandard.INSTANCE.getScenarioSuitesByStandardVersion().keySet().stream()
            .findFirst()
            .orElseThrow(),
        EblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE_TD_ONLY,
        EblRole.CARRIER.getConfigName());
  }
}
