package org.dcsa.conformance.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;
import org.junit.jupiter.api.Test;

@Slf4j
class ManualEBlIssuanceTest extends ManualTestBase {

  public ManualEBlIssuanceTest() {
    super(log); // Make sure no log lines are logged with the Base class logger
  }

  @Test
  void testManualEBLFlowFirstScenario() {
    getAllSandboxes();
    getAvailableStandards();

    SandboxConfig sandbox1 =
        createSandbox(
            new Sandbox(
                "eBL Issuance",
                "3.0.0",
                "Conformance",
                "Carrier",
                true,
                "eBL Issuance - Carrier testing: orchestrator"));

    SandboxConfig sandbox2 =
        createSandbox(
            new Sandbox(
                "eBL Issuance",
                "3.0.0",
                "Conformance",
                "Carrier",
                false,
                "eBL Issuance - Carrier testing: synthetic carrier as tested party"));

    updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId());
    assertEquals(1, sandbox1Digests.size());
    assertTrue(sandbox1Digests.getFirst().scenarios().size() >= 13);

    updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId());
    assertTrue(sandbox2Digests.isEmpty());

    Scenario scenario = sandbox1Digests.getFirst().scenarios().getFirst();
    runScenarioSupplyCarrierParameters(sandbox1, sandbox2, scenario.id(), scenario.name());
  }

  private void runScenarioSupplyCarrierParameters(
      SandboxConfig sandbox1, SandboxConfig sandbox2, String scenarioId, String scenarioName) {
    startOrStopScenario(sandbox1, scenarioId);

    JsonNode jsonNode = getScenarioStatus(sandbox1, scenarioId);
    String promptActionId = jsonNode.get("promptActionId").textValue();

    // Send Action input -- @Sandbox 1
    JsonNode textInputNode =
        mapper
            .createObjectNode()
            .put("carrierSigningKeyPEM", PayloadSignerFactory.getCarrierPublicKeyInPemFormat());
    handleActionInput(sandbox1, scenarioId, promptActionId, textInputNode);
    if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 2);

    notifyAction(sandbox2);

    validateSandboxStatus(sandbox1, scenarioId, 0, "Request(duplicate)");
    completeAction(sandbox1);
    validateSandboxStatus(sandbox1, scenarioId, 1, "Response(ISSU)");
    completeAction(sandbox1);
    validateSandboxScenarioGroup(sandbox1, scenarioId, scenarioName);
  }
}
