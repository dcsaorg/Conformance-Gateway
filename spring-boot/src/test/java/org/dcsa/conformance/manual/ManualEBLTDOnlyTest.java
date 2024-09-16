package org.dcsa.conformance.manual;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class ManualEBLTDOnlyTest extends ManualTestBase {

  @Disabled("A required API exchange was not yet detected for action 'TD Change (Out of Band)'")
  @ParameterizedTest
  @CsvSource({"Carrier", "Shipper"})
  void testManualEblTD(String testedParty) {
    app.setSimulatedLambdaDelay(lambdaDelay);
    getAllSandboxes();
    getAvailableStandards();

    SandboxConfig sandbox1 =
      createSandbox(
        new Sandbox(
          "Ebl",
          "3.0.0",
          "Conformance TD-only",
          testedParty,
          true,
          "Ebl-TD - %s testing: orchestrator".formatted(testedParty)));
    SandboxConfig sandbox2 =
      createSandbox(
        new Sandbox(
          "Ebl",
          "3.0.0",
          "Conformance TD-only",
          testedParty,
          false,
          "Ebl-TD - %s testing: synthetic %s as tested party"
            .formatted(testedParty, testedParty)));

    updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId());
    assertTrue(sandbox1Digests.size() >= 2);

    updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId());
    assertTrue(sandbox2Digests.isEmpty());

    // Run all tests for all scenarios
    runAllTests(sandbox1Digests, sandbox1, sandbox2);

    log.info("Run for the 2nd time, and see that it still works");
    runAllTests(sandbox1Digests, sandbox1, sandbox2);
  }

}
