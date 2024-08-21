package org.dcsa.conformance.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class ManualBookingTest extends ManualTestBase {

  public ManualBookingTest() {
    super(log); // Make sure no log lines are logged with the Base class logger
  }

  @Test
  void testManualBookingFlowFirstScenario() {
    app.setSimulatedLambdaDelay(lambdaDelay);
    getAllSandboxes();
    getAvailableStandards();

    SandboxConfig sandbox1 =
        createSandbox(
            new Sandbox(
                "Booking",
                "2.0.0",
                "Conformance",
                "Carrier",
                true,
                "Carrier testing: orchestrator"));
    SandboxConfig sandbox2 =
        createSandbox(
            new Sandbox(
                "Booking",
                "2.0.0",
                "Conformance",
                "Carrier",
                false,
                "Carrier testing: synthetic carrier as tested party"));

    updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId());
    assertTrue(sandbox1Digests.size() >= 3);

    updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId());
    assertTrue(sandbox2Digests.isEmpty());

    runScenario(sandbox1, sandbox2, sandbox1Digests.getFirst().scenarios().getFirst().id());
    // Run for the 2nd time, and see that it still works
    runScenario(sandbox1, sandbox2, sandbox1Digests.getFirst().scenarios().getFirst().id());
  }

  private void runScenario(SandboxConfig sandbox1, SandboxConfig sandbox2, String scenarioId) {
    // Start the scenario 1 -- Dry Cargo
    startOrStopScenario(sandbox1, scenarioId);
    notifyAction(sandbox2);

    // Get getScenarioStatus -- @Sandbox 1
    JsonNode jsonNode = getScenarioStatus(sandbox1, scenarioId);
    String jsonForPromptText = jsonNode.get("jsonForPromptText").toString();
    assertTrue(jsonForPromptText.length() > 250);
    String promptActionId = jsonNode.get("promptActionId").textValue();

    // Send Action input -- @Sandbox 1
    handleActionInput(sandbox1, scenarioId, promptActionId, jsonNode);
    if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 2);

    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 0, "UC1");
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 1, "GET");

    notifyAction(sandbox2);
    completeAction(sandbox1);
    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 2, "UC2");
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 3, "GET");

    notifyAction(sandbox2);
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 4, "UC3");
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 5, "GET");

    notifyAction(sandbox2);
    completeAction(sandbox1);
    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 6, "UC5");
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 7, "GET");

    notifyAction(sandbox2);
    completeAction(sandbox1);
    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 8, "UC12");
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 9, "GET");
    completeAction(sandbox1);

    validateSandboxScenarioGroup(sandbox1, scenarioId);
  }
}
