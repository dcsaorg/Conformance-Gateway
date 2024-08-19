package org.dcsa.conformance.springboot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.HttpHeaderConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ConformanceApplicationWebUITest {

  private final ObjectMapper mapper = new ObjectMapper();
  private static final String USER_ID = "unit-test";

  @Autowired
  private ConformanceApplication app;

  @Test
  void testManualFlow() {
    ObjectNode node = mapper.createObjectNode().put("operation", "getAllSandboxes");
    JsonNode jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.size() >= 6);

    node = mapper.createObjectNode().put("operation", "getAvailableStandards");
    jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.size() >= 6);

    SandboxConfig sandbox1 = createSandbox(true, "Carrier testing: orchestrator");
    SandboxConfig sandbox2 = createSandbox(false, "Carrier testing: synthetic carrier as tested party");

    updateSandboxConfigBeforeStarting(sandbox1, sandbox2);
    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId);
    assertTrue(sandbox1Digests.size() >= 3);

    updateSandboxConfigBeforeStarting(sandbox2, sandbox1);
    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId);
    assertTrue(sandbox2Digests.isEmpty());

    runScenario1(sandbox1, sandbox2, sandbox1Digests);
  }

  private void runScenario1(SandboxConfig sandbox1, SandboxConfig sandbox2, List<ScenarioDigest> sandbox1Digests) {
    // Start the scenario 1 -- @Sandbox 1
    ObjectNode node = mapper.createObjectNode().put("operation", "startOrStopScenario").put("sandboxId", sandbox1.sandboxId).put("scenarioId", sandbox1Digests.getFirst().scenarios().getFirst().id());
    assertTrue(app.webuiHandler.handleRequest(USER_ID, node).isEmpty());

    // Notify party -- @Sandbox 2
    node = mapper.createObjectNode().put("operation", "notifyParty").put("sandboxId", sandbox2.sandboxId);
    assertTrue(app.webuiHandler.handleRequest(USER_ID, node).isEmpty());

    // Get getScenarioStatus -- @Sandbox 1
    node = mapper.createObjectNode().put("operation", "getScenarioStatus").put("sandboxId", sandbox1.sandboxId).put("scenarioId", sandbox1Digests.getFirst().scenarios().getFirst().id());
    JsonNode jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    String jsonForPromptText = jsonNode.get("jsonForPromptText").toString();
    assertTrue(jsonForPromptText.length() > 250);
    String promptActionId = jsonNode.get("promptActionId").textValue();

    // Send Action input -- @Sandbox 1
    node = mapper.createObjectNode().put("operation", "handleActionInput").put("sandboxId", sandbox1.sandboxId).put("scenarioId", sandbox1Digests.getFirst().scenarios().getFirst().id())
      .put("actionId", promptActionId)
      .set("actionInput", jsonNode.get("jsonForPromptText"));
    jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.isEmpty(), "Should be empty, found: " + jsonNode);

    // Notify party -- @Sandbox 2
    node = mapper.createObjectNode().put("operation", "notifyParty").put("sandboxId", sandbox2.sandboxId);
    assertTrue(app.webuiHandler.handleRequest(USER_ID, node).isEmpty());

    // TODO: remove the sleep, and use a polling mechanism
    try {
      Thread.sleep(500); // Wait for the scenario to finish
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    // Refresh and check -- @Sandbox 2
    node = mapper.createObjectNode().put("operation", "getSandbox").put("sandboxId", sandbox2.sandboxId).put("includeOperatorLog", true);
    jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    String operatorLog = jsonNode.get("operatorLog").get(0).asText();
    assertTrue(operatorLog.contains("Responded to request 'POST"));
    assertTrue(operatorLog.contains("with '201'"));

    // Validate getScenarioStatus -- @Sandbox 1
    node = mapper.createObjectNode().put("operation", "getScenarioStatus").put("sandboxId", sandbox1.sandboxId).put("scenarioId", sandbox1Digests.getFirst().scenarios().getFirst().id());
    jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.get("isRunning").asBoolean());
    JsonNode conformanceSubReport = jsonNode.get("conformanceSubReport").get("subReports").iterator().next();
    assertEquals("CONFORMANT", conformanceSubReport.get("status").asText());
    assertTrue(conformanceSubReport.get("errorMessages").isEmpty(), "Should be empty, but found: " + conformanceSubReport.get("errorMessages"));
  }

  private void updateSandboxConfigBeforeStarting(SandboxConfig sandbox1, SandboxConfig sandbox2) {
    // Update Sandbox Config 1, with details from Sandbox Config 2
    JsonNode node = mapper.createObjectNode().put("operation", "updateSandboxConfig").put("sandboxId", sandbox1.sandboxId)
      .put("sandboxName", "Carrier")
      .put("externalPartyUrl", sandbox2.sandboxUrl)
      .put("externalPartyAuthHeaderName", sandbox2.sandboxAuthHeaderName)
      .put("externalPartyAuthHeaderValue", sandbox2.sandboxAuthHeaderValue);
    assertTrue(app.webuiHandler.handleRequest(USER_ID, node).isEmpty());

    // Get Sandbox 1 details
    node = mapper.createObjectNode().put("operation", "getSandbox").put("sandboxId", sandbox1.sandboxId).put("includeOperatorLog", true);
    JsonNode jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.has("id"));
    assertTrue(jsonNode.has("name"));
    assertTrue(jsonNode.has("operatorLog"));
    assertEquals(jsonNode.get("id").asText(), sandbox1.sandboxId);
  }

  private SandboxConfig createSandbox(boolean isDefaultType, String sandboxName) {
    JsonNode node = mapper.createObjectNode().put("operation", "createSandbox")
      .put("standardName", "Booking")
      .put("versionNumber", "2.0.0")
      .put("scenarioSuite", "Conformance")
      .put("testedPartyRole", "Carrier")
      .put("isDefaultType", isDefaultType)
      .put("sandboxName", sandboxName);
    JsonNode jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    log.info("Response: {}", jsonNode);
    assertTrue(jsonNode.has("sandboxId"));
    String sandboxId = jsonNode.get("sandboxId").asText();

    // Get the sandbox config
    node = mapper.createObjectNode().put("operation", "getSandboxConfig").put("sandboxId", sandboxId);
    jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    return mapper.convertValue(jsonNode, SandboxConfig.class);
  }

  private List<ScenarioDigest> getScenarioDigests(String sandboxId) {
    JsonNode node = mapper.createObjectNode().put("operation", "getScenarioDigests").put("sandboxId", sandboxId);
    JsonNode jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.isArray());
    return mapper.convertValue(jsonNode, new TypeReference<>() {
    });
  }

  record SandboxConfig(String sandboxId, String sandboxName, String sandboxUrl, String sandboxAuthHeaderName,
                       String sandboxAuthHeaderValue, String externalPartyUrl, String externalPartyAuthHeaderName,
                       String externalPartyAuthHeaderValue, HttpHeaderConfiguration[] externalPartyAdditionalHeaders) {
  }

  record ScenarioDigest(String moduleName, List<Scenario> scenarios) {
  }

  record Scenario(String id, String name, boolean isRunning, String conformanceStatus) {
  }

}
