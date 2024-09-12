package org.dcsa.conformance.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class ManualEBlIssuanceTest extends ManualTestBase {

  public ManualEBlIssuanceTest() {
    super(log); // Make sure no log lines are logged with the Base class logger
  }

  @Disabled("Disabled: Carrier issue: Signature of the issuanceManifestSignedContent is valid. Platform issue: STRAIGHT_EBL -> Response status '409' does not match the expected value '204'")
  @ParameterizedTest
  @CsvSource({"Carrier", "Platform"})
  void testManualEBLIssuance(String testedParty) {
    getAllSandboxes();
    getAvailableStandards();

    SandboxConfig sandbox1 =
        createSandbox(
            new Sandbox(
                "eBL Issuance",
                "3.0.0",
                "Conformance",
                testedParty,
                true,
                "eBL Issuance - %s testing: orchestrator".formatted(testedParty)));

    SandboxConfig sandbox2 =
        createSandbox(
            new Sandbox(
                "eBL Issuance",
                "3.0.0",
                "Conformance",
                testedParty,
                false,
                "eBL Issuance - %s testing: synthetic %s as tested party"
                  .formatted(testedParty, testedParty)));

    updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId());
    assertEquals(1, sandbox1Digests.size());
    assertTrue(sandbox1Digests.getFirst().scenarios().size() >= 13, "Found: " + sandbox1Digests.size());

    updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId());
    assertTrue(sandbox2Digests.isEmpty());

    // Run all tests for all scenarios
    runAllTests(sandbox1Digests, sandbox1, sandbox2);

    log.info("Run for the 2nd time, and see that it still works");
    runAllTests(sandbox1Digests, sandbox1, sandbox2);
  }

}
