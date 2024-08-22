package org.dcsa.conformance.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class ManualEBLSIOnlyTest extends ManualTestBase {

  public ManualEBLSIOnlyTest() {
    super(log); // Make sure no log lines are logged with the Base class logger
  }

  @Test
  void testManualEBLFlowFirstScenario() {
    getAllSandboxes();
    getAvailableStandards();

    SandboxConfig sandbox1 =
        createSandbox(
            new Sandbox(
                "Ebl",
                "3.0.0",
                "Conformance SI-only",
                "Carrier",
                true,
                "Ebl-SI - Carrier testing: orchestrator"));

    SandboxConfig sandbox2 =
        createSandbox(
            new Sandbox(
                "Ebl",
                "3.0.0",
                "Conformance SI-only",
                "Carrier",
                false,
                "Ebl-SI - Carrier testing: synthetic carrier as tested party"));

    updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId());
    assertEquals(3, sandbox1Digests.size());
    assertTrue(sandbox1Digests.getFirst().scenarios().size() >= 9);

    updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId());
    assertTrue(sandbox2Digests.isEmpty());

    // Run all tests on: Supported shipment types scenarios
    for (int i = 0; i < 9; i++) {
      runScenario(sandbox1, sandbox2, sandbox1Digests.getFirst().scenarios().get(i).id());
    }
  }

  private void runScenario(SandboxConfig sandbox1, SandboxConfig sandbox2, String scenarioId) {
    startOrStopScenario(sandbox1, scenarioId);

    // Get getScenarioStatus
    JsonNode jsonNode = getScenarioStatus(sandbox1, scenarioId);
    String jsonForPromptText = jsonNode.get("jsonForPromptText").toString();
    assertTrue(jsonForPromptText.length() > 250);
    String promptActionId = jsonNode.get("promptActionId").textValue();

    handleActionInput(sandbox1, scenarioId, promptActionId, jsonNode.get("jsonForPromptText"));
    if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 2);

    validateSandboxStatus(sandbox1, scenarioId, 0, "UC1");

    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 1, "GET SI");

    notifyAction(sandbox2);
    completeAction(sandbox1);
    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 2, "UC14");

    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 3, "GET SI");

    completeAction(sandbox1);
    validateSandboxScenarioGroup(sandbox1, scenarioId);
  }
}
