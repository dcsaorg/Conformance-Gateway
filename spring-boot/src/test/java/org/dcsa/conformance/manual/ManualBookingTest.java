package org.dcsa.conformance.manual;

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
                "Booking - Carrier testing: orchestrator"));
    SandboxConfig sandbox2 =
        createSandbox(
            new Sandbox(
                "Booking",
                "2.0.0",
                "Conformance",
                "Carrier",
                false,
                "Booking - Carrier testing: synthetic carrier as tested party"));

    updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId());
    assertTrue(sandbox1Digests.size() >= 3);

    updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId());
    assertTrue(sandbox2Digests.isEmpty());

    Scenario scenario1 = sandbox1Digests.getFirst().scenarios().getFirst();
    runDryCargoScenario(sandbox1, sandbox2, scenario1.id(), scenario1.name());

    // Run all tests on: Dangerous goods scenarios
    // TODO: turn on when all scenarios are implemented. Now only the first one is implemented. Line
    // 52 can be removed.
    /*sandbox1Digests
    .get(0)
    .scenarios()
    .forEach(
      scenario ->
        runDryCargoScenario(sandbox1, sandbox2, scenario.id(), scenario.name()));*/

    // Run for the 2nd time, and see that it still works
    runDryCargoScenario(sandbox1, sandbox2, scenario1.id(), scenario1.name());

    // Run all tests on: Dangerous goods scenarios
    sandbox1Digests
        .get(1)
        .scenarios()
        .forEach(
            scenario ->
                runDangerousGoodsScenario(sandbox1, sandbox2, scenario.id(), scenario.name()));

    // Run all tests on: Reefer containers
    sandbox1Digests
        .get(2)
        .scenarios()
        .forEach(
            scenario ->
                runReeferContainersScenario(sandbox1, sandbox2, scenario.id(), scenario.name()));

    // Validate all scenarios are completed and conformant
    // TODO: turn on when all scenarios are implemented
    /*sandbox1Digests.forEach(
    scenarioDigest -> {
      log.info("Validating Module '{}' was tested properly.", scenarioDigest.moduleName());
      scenarioDigest
        .scenarios()
        .forEach(scenario -> validateSandboxScenarioGroup(sandbox1, scenario.id(), scenario.name()));
    });*/
  }

  private void runDryCargoScenario(
      SandboxConfig sandbox1, SandboxConfig sandbox2, String scenarioId, String scenarioName) {
    startOrStopScenario(sandbox1, scenarioId);
    notifyAction(sandbox2);

    JsonNode jsonNode = getScenarioStatus(sandbox1, scenarioId);
    String jsonForPromptText = jsonNode.get("jsonForPromptText").toString();
    assertTrue(jsonForPromptText.length() > 250);
    String promptActionId = jsonNode.get("promptActionId").textValue();

    handleActionInput(sandbox1, scenarioId, promptActionId, jsonNode.get("jsonForPromptText"));
    if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 2);

    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 0, "UC1");
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 1, "GET");

    completeAction(sandbox1);
    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 2, "UC2");
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 3, "GET");

    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 4, "UC3");
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 5, "GET");

    completeAction(sandbox1);
    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 6, "UC5");
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 7, "GET");

    completeAction(sandbox1);
    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 8, "UC12");
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 9, "GET");
    completeAction(sandbox1);

    validateSandboxScenarioGroup(sandbox1, scenarioId, scenarioName);
  }

  private void runDangerousGoodsScenario(
      SandboxConfig sandbox1, SandboxConfig sandbox2, String scenarioId, String scenarioName) {
    // TODO: implement all Dangerous Goods scenarios
  }

  private void runReeferContainersScenario(
      SandboxConfig sandbox1, SandboxConfig sandbox2, String scenarioId, String scenarioName) {
    // TODO: implement all Reefer Containers scenarios
  }
}
