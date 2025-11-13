package org.dcsa.conformance.manual;

import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.springboot.ConformanceApplication;
import org.dcsa.conformance.standards.ebl.EblScenarioListBuilder;
import org.dcsa.conformance.standards.ebl.EblStandard;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = ConformanceApplication.class)
class ManualScenarioWithNotificationsTest extends ManualTestBase {

  @SuppressWarnings("unused")
  private static Stream<Arguments> testStandards() {
    return Stream.of(
        Arguments.of("Adoption", false),
        Arguments.of("Adoption", true),
        Arguments.of("AN", false),
        Arguments.of("AN", true),
        Arguments.of("Booking", false),
        Arguments.of("Booking", true),
        Arguments.of("CS", false),
        Arguments.of("CS", true),
        Arguments.of("Ebl", false),
        Arguments.of("Ebl", true),
        // TODO re-enable after SD-2491
        // Arguments.of("JIT", false),
        // Arguments.of("JIT", true),
        Arguments.of("OVS", false),
        Arguments.of("OVS", true),
        Arguments.of("PINT", false),
        Arguments.of("PINT", true),
        Arguments.of("TnT", false),
        Arguments.of("TnT", true),
        Arguments.of("eBL Issuance", false),
        Arguments.of("eBL Issuance", true),
        Arguments.of("eBL Surrender", false),
        Arguments.of("eBL Surrender", true),
        Arguments.of("Booking + eBL", false),
        Arguments.of("Booking + eBL", true),
        Arguments.of("VGM", true));
  }

  @ParameterizedTest(name = "Standard: {0} - 2nd run: {1}")
  @MethodSource("testStandards")
  void testStandards(String standardName, boolean secondRun) {
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
                                            role.name(),
                                            secondRun))));
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
        EblRole.CARRIER.getConfigName(),
        false);
  }
}
