package org.dcsa.conformance.manual;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.springboot.ConformanceApplication;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = ConformanceApplication.class)
class ManualScenarioTest extends ManualTestBase {

  @SuppressWarnings("unused")
  private static Stream<Arguments> testStandards() {
    return Stream.of(
        Arguments.of("Adoption", false),
        Arguments.of("Adoption", true),
        Arguments.of("Booking", false),
        Arguments.of("Booking", true),
        Arguments.of("CS", false),
        Arguments.of("CS", true),
        Arguments.of("Ebl", false),
        Arguments.of("Ebl", true),
        Arguments.of("JIT", false),
        Arguments.of("JIT", true),
        Arguments.of("OVS", false),
        Arguments.of("OVS", true),
        Arguments.of("PINT", false),
        Arguments.of("PINT", true),
        Arguments.of("TnT", false),
        Arguments.of("TnT", true),
        Arguments.of("eBL Issuance", false),
        Arguments.of("eBL Issuance", true),
        Arguments.of("eBL Surrender", false),
        Arguments.of("eBL Surrender", true)
    );
  }

  @ParameterizedTest(name = "Standard: {0} - 2nd run: {1}")
  @MethodSource("testStandards")
  void testStandards(String standardName, boolean secondRun) {
    app.setSimulatedLambdaDelay(lambdaDelay);
    getAllSandboxes();
    List<Standard> availableStandards = getAvailableStandards();
    Standard standard1 =
        availableStandards.stream()
            .filter(standard -> standard.name().equals(standardName))
            .findFirst()
            .orElseThrow();

    standard1
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
                                            standard1.name(), version.number(), suite, role, secondRun))));
  }

  @Test
  @Disabled("Only for debugging")
  void testOnlyOneSpecificScenario(){
    runManualTests("Booking", "2.0.0", "Conformance", "Carrier", false);
  }

  private void runManualTests(
      String standardName, String standardVersion, String suiteName, String roleName, boolean secondRun) {
    SandboxConfig sandbox1;
    SandboxConfig sandbox2;
    if (!secondRun) {
      sandbox1 =
          createSandbox(
              new Sandbox(
                  standardName,
                  standardVersion,
                  suiteName,
                  roleName,
                  true,
                  getSandboxName(standardName, standardVersion, suiteName, roleName, 0)));
      sandbox2 =
          createSandbox(
              new Sandbox(
                  standardName,
                  standardVersion,
                  suiteName,
                  roleName,
                  false,
                  getSandboxName(standardName, standardVersion, suiteName, roleName, 1)));
      updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
      updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    } else {
      sandbox1 =
          getSandboxByName(getSandboxName(standardName, standardVersion, suiteName, roleName, 0));
      sandbox2 =
          getSandboxByName(getSandboxName(standardName, standardVersion, suiteName, roleName, 1));
      log.info("Run for the 2nd time, and verify it still works.");
      log.info("Using sandboxes: {} v{}, suite: {}, role: {}", standardName, standardVersion, suiteName, roleName);
      resetSandbox(sandbox2); // Make sure the sandbox does not keep an optional state from the first run
    }

    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId());
    assertFalse(sandbox1Digests.isEmpty(), "No scenarios found?");

    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId());
    assertTrue(sandbox2Digests.isEmpty(), "Scenarios found?");
    getAllSandboxes();

    // Run all tests for all scenarios
    runAllTests(sandbox1Digests, sandbox1, sandbox2);
    log.info("Done with {} as role: {}", standardName, roleName);
  }
}
