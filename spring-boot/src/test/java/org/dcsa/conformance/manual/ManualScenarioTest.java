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

  // PINT is postponed, until it has non-RI impls.
  @SuppressWarnings("unused")
  private static Stream<Arguments> testStandards() {
    return Stream.of(
        Arguments.of("Adoption", false),
        Arguments.of("Adoption", true),
        Arguments.of("Booking", false),
        Arguments.of("Booking", true),
        Arguments.of("CS", false),
        Arguments.of("CS", true),
        Arguments.of("JIT", false),
        Arguments.of("JIT", true),
        Arguments.of("OVS", false),
        Arguments.of("OVS", true),
        Arguments.of("TnT", false),
        Arguments.of("TnT", true)

      // eBL Issuance: 1) Carrier issue: Signature of the issuanceManifestSignedContent is valid.
      // eBL Issuance: 2) Platform issue: STRAIGHT_EBL -> Response status '409' does not match the expected value '204'
//        Arguments.of("eBL Issuance", false),
//        Arguments.of("eBL Issuance", true),

      // eBL Surrender: 1) Carrier issue: A required API exchange was not yet detected for action 'Void&Reissue'.
      // eBL Surrender: 2) Platform: same issue on 'SURR 409'
//        Arguments.of("eBL Surrender", false),
//        Arguments.of("eBL Surrender", true)
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
                version
                    .roles()
                    .forEach(role -> runManualTests(standard1.name(), version, role, secondRun)));
  }

  private void runManualTests(
      String standardName, StandardVersion version, String roleName, boolean secondRun) {
    SandboxConfig sandbox1;
    SandboxConfig sandbox2;
    if (!secondRun) {
      sandbox1 =
          createSandbox(
              new Sandbox(
                  standardName,
                  version.number(),
                  "Conformance",
                  roleName,
                  true,
                  "%s - %s testing: orchestrator".formatted(standardName, roleName)));
      sandbox2 =
          createSandbox(
              new Sandbox(
                  standardName,
                  version.number(),
                  "Conformance",
                  roleName,
                  false,
                  "%s - %s testing: synthetic %s as tested party"
                      .formatted(standardName, roleName, roleName)));
      updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
      updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    } else {
      sandbox1 =
          getSandboxByName("%s - %s testing: orchestrator".formatted(standardName, roleName));
      sandbox2 =
          getSandboxByName(
              "%s - %s testing: synthetic %s as tested party"
                  .formatted(standardName, roleName, roleName));
      log.info("Run for the 2nd time, and see that it still works");
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
