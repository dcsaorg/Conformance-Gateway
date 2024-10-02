package org.dcsa.conformance.manual;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
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

//      DT-1681, issue with Ebl v3.0.0, suite: Conformance TD-only, Carrier & Shipper
//        Arguments.of("Ebl", false),
//        Arguments.of("Ebl", true),
        Arguments.of("JIT", false),
        Arguments.of("JIT", true),
        Arguments.of("OVS", false),
        Arguments.of("OVS", true),
//       DT-1796, PINT issue: Unknown TD Reference: j5k./hY,wZNqNDSEmJ`
//        Arguments.of("PINT", true),
//        Arguments.of("PINT", false),
        Arguments.of("TnT", false),
        Arguments.of("TnT", true),
        Arguments.of("eBL Issuance", false)

//       2nd run of eBL Issuance is not working, DT-1679: Scenario 'Supply carrier parameters - Supply scenario parameters
//       [STRAIGHT_EBL] - Request(duplicate) - Response(ISSU)' is not conformant. Details: Supply carrier parameters -
//       Supply scenario parameters [STRAIGHT_EBL] - Request(duplicate) - Response(ISSU): NON_CONFORMANT
//        Arguments.of("eBL Issuance", true),

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
                                            standard1.name(), version, suite, role, secondRun))));
  }

  private void runManualTests(
      String standardName, StandardVersion version, String suiteName, String roleName, boolean secondRun) {
    SandboxConfig sandbox1;
    SandboxConfig sandbox2;
    if (!secondRun) {
      sandbox1 =
          createSandbox(
              new Sandbox(
                  standardName,
                  version.number(),
                  suiteName,
                  roleName,
                  true,
                  getSandboxName(standardName, version.number(), suiteName, roleName, 0)));
      sandbox2 =
          createSandbox(
              new Sandbox(
                  standardName,
                  version.number(),
                  suiteName,
                  roleName,
                  false,
                  getSandboxName(standardName, version.number(), suiteName, roleName, 1)));
      updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
      updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    } else {
      sandbox1 =
          getSandboxByName(getSandboxName(standardName, version.number(), suiteName, roleName, 0));
      sandbox2 =
          getSandboxByName(getSandboxName(standardName, version.number(), suiteName, roleName, 1));
      log.info("Run for the 2nd time, and verify it still works.");
      log.info("Using sandboxes: {} v{}, suite: {}, role: {}", standardName, version.number(), suiteName, roleName);
    }

    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId());
    assertFalse(sandbox1Digests.isEmpty());

    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId());
    assertTrue(sandbox2Digests.isEmpty());
    getAllSandboxes();

    // Run all tests for all scenarios
    runAllTests(sandbox1Digests, sandbox1, sandbox2);
    log.info("Done with {} as role: {}", standardName, roleName);
  }
}
