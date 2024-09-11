package org.dcsa.conformance.manual;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class ManualBookingTest extends ManualTestBase {

  public ManualBookingTest() {
    super(log); // Make sure no log lines are logged with the Base class logger
  }

  @ParameterizedTest
  @CsvSource({"Carrier", "Shipper"})
  void testManualBooking(String testedParty) {
    app.setSimulatedLambdaDelay(lambdaDelay);
    getAllSandboxes();
    getAvailableStandards();

    SandboxConfig sandbox1 =
        createSandbox(
            new Sandbox(
                "Booking",
                "2.0.0",
                "Conformance",
                testedParty,
                true,
                "Booking - %s testing: orchestrator".formatted(testedParty)));
    SandboxConfig sandbox2 =
        createSandbox(
            new Sandbox(
                "Booking",
                "2.0.0",
                "Conformance",
                testedParty,
                false,
                "Booking - %s testing: synthetic %s as tested party"
                    .formatted(testedParty, testedParty)));

    updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId());
    assertTrue(sandbox1Digests.size() >= 3);

    updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId());
    assertTrue(sandbox2Digests.isEmpty());

    // Run all tests for all scenarios
    runAllTests(sandbox1Digests, sandbox1, sandbox2);

    log.info("Run for the 2nd time, and see that it still works");
    runAllTests(sandbox1Digests, sandbox1, sandbox2);
  }

  private void runAllTests(
      List<ScenarioDigest> sandbox1Digests, SandboxConfig sandbox1, SandboxConfig sandbox2) {
    sandbox1Digests.forEach(
        scenarioDigest ->
            scenarioDigest
                .scenarios()
                .forEach(
                    scenario -> runScenario(sandbox1, sandbox2, scenario.id(), scenario.name())));
  }

  private void runScenario(
      SandboxConfig sandbox1, SandboxConfig sandbox2, String scenarioId, String scenarioName) {

    startOrStopScenario(sandbox1, scenarioId);
    notifyAction(sandbox2);

    boolean isRunning;
    do {
      JsonNode jsonNode = getScenarioStatus(sandbox1, scenarioId);
      boolean inputRequired = jsonNode.get("inputRequired").booleanValue();
      boolean hasPromptText = jsonNode.has("promptText");
      isRunning = jsonNode.get("isRunning").booleanValue();
      if (inputRequired) {
        String jsonForPromptText = jsonNode.get("jsonForPromptText").toString();
        assertTrue(jsonForPromptText.length() > 250);
        String promptActionId = jsonNode.get("promptActionId").textValue();

        handleActionInput(sandbox1, scenarioId, promptActionId, jsonNode.get("jsonForPromptText"));
        if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 2);
        continue;
      }
      if (hasPromptText && !jsonNode.get("promptText").textValue().isEmpty()) {
        notifyAction(sandbox2);
      }
      if (isRunning) completeAction(sandbox1);
    } while (isRunning);
    validateSandboxScenarioGroup(sandbox1, scenarioId, scenarioName);
  }
}
