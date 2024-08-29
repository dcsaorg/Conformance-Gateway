package org.dcsa.conformance.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class ManualEBLTDOnlyTest extends ManualTestBase {

  public ManualEBLTDOnlyTest() {
    super(log); // Make sure no log lines are logged with the Base class logger
  }

  @Test
  void testTDScenario() {
    getAllSandboxes();
    getAvailableStandards();

    SandboxConfig sandbox1 =
        createSandbox(
            new Sandbox(
                "Ebl",
                "3.0.0",
                "Conformance TD-only",
                "Carrier",
                true,
                "Ebl-TD - Carrier testing: orchestrator"));

    SandboxConfig sandbox2 =
        createSandbox(
            new Sandbox(
                "Ebl",
                "3.0.0",
                "Conformance TD-only",
                "Carrier",
                false,
                "Ebl-TD - Carrier testing: synthetic carrier as tested party"));

    updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId());
    assertEquals(2, sandbox1Digests.size());
    assertTrue(sandbox1Digests.getFirst().scenarios().size() >= 9);

    updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId());
    assertTrue(sandbox2Digests.isEmpty());

    // Run all tests on: Supported shipment types scenarios
    sandbox1Digests
        .getFirst()
        .scenarios()
        .forEach(
            scenario ->
                runSupportedShipmentScenario(sandbox1, sandbox2, scenario.id(), scenario.name()));

    // Run all tests on: Shipper interactions with transport document
    sandbox1Digests
        .get(1)
        .scenarios()
        .forEach(
            scenario ->
                runShipperInteractionsScenario(sandbox1, sandbox2, scenario.id(), scenario.name()));

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

  private void runSupportedShipmentScenario(
      SandboxConfig sandbox1, SandboxConfig sandbox2, String scenarioId, String scenarioName) {
    startOrStopScenario(sandbox1, scenarioId);

    // Get getScenarioStatus
    JsonNode jsonNode = getScenarioStatus(sandbox1, scenarioId);
    String jsonForPromptText = jsonNode.get("jsonForPromptText").toString();
    assertTrue(jsonForPromptText.length() > 250);
    String promptActionId = jsonNode.get("promptActionId").textValue();

    handleActionInput(sandbox1, scenarioId, promptActionId, jsonNode.get("jsonForPromptText"));
    if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 2);

    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 0, "UC6");

    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 1, "GET TD");

    completeAction(sandbox1);
    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 2, "UC8");

    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 3, "GET TD");

    completeAction(sandbox1);
    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 4, "UC12");

    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 5, "GET TD");

    completeAction(sandbox1);
    notifyAction(sandbox2);
    validateSandboxStatus(sandbox1, scenarioId, 6, "UC13a");

    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 7, "GET TD");

    completeAction(sandbox1);
    validateSandboxScenarioGroup(sandbox1, scenarioId, scenarioName);
  }

  private void runShipperInteractionsScenario(
      SandboxConfig sandbox1, SandboxConfig sandbox2, String scenarioId, String scenarioName) {
    // TODO: implement all Shipper interactions with transport document scenarios.
  }
}
