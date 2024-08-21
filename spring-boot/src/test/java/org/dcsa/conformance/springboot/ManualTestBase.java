package org.dcsa.conformance.springboot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.party.HttpHeaderConfiguration;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Base class which contains all API call methods and wiring needed to perform a manual test */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public abstract class ManualTestBase {
  private static final String USER_ID = "unit-test";

  private final ObjectMapper mapper = new ObjectMapper();
  protected final long lambdaDelay = 0L;
  private final Logger log;

  public ManualTestBase(Logger log) {
    this.log = log;
  }

  @Autowired protected ConformanceApplication app;

  void getAvailableStandards() {
    ObjectNode node = mapper.createObjectNode().put("operation", "getAvailableStandards");
    JsonNode jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.size() >= 6);
  }

  void getAllSandboxes() {
    ObjectNode node = mapper.createObjectNode().put("operation", "getAllSandboxes");
    JsonNode jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.size() >= 6);
  }

  void handleActionInput(
      SandboxConfig sandbox, String scenarioId, String promptActionId, JsonNode jsonNode) {
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "handleActionInput")
            .put("sandboxId", sandbox.sandboxId)
            .put("scenarioId", scenarioId)
            .put("actionId", promptActionId)
            .set("actionInput", jsonNode.get("jsonForPromptText"));
    jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.isEmpty(), "Should be empty, found: " + jsonNode);
  }

  void startOrStopScenario(SandboxConfig sandbox, String scenarioId) {
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "startOrStopScenario")
            .put("sandboxId", sandbox.sandboxId)
            .put("scenarioId", scenarioId);
    assertTrue(app.webuiHandler.handleRequest(USER_ID, node).isEmpty());
  }

  void notifyAction(SandboxConfig sandbox) {
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "notifyParty")
            .put("sandboxId", sandbox.sandboxId);
    assertTrue(app.webuiHandler.handleRequest(USER_ID, node).isEmpty());
    waitForCleanSandboxStatus(sandbox);
    waitForAsyncCalls(250L);
  }

  void completeAction(SandboxConfig sandbox) {
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "completeCurrentAction")
            .put("sandboxId", sandbox.sandboxId);
    JsonNode jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.isEmpty(), "Should be empty, found: " + jsonNode);
    waitForCleanSandboxStatus(sandbox);
  }

  void validateSandboxStatus(
      SandboxConfig sandbox1, String scenarioId, int useCaseIndex, String expectedTitle) {
    log.info("Validating scenario status for use case: {} (#{})", expectedTitle, useCaseIndex + 1);
    if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 4);
    JsonNode jsonNode = getScenarioStatus(sandbox1, scenarioId);
    assertTrue(jsonNode.get("isRunning").asBoolean());
    JsonNode conformanceSubReport =
        jsonNode.get("conformanceSubReport").get("subReports").get(useCaseIndex);
    assertEquals(
        "CONFORMANT",
        conformanceSubReport.get("status").asText(),
        "Found in use case #" + useCaseIndex + " with title '" + expectedTitle + "'. ");
    assertEquals(expectedTitle, conformanceSubReport.get("title").asText());
    assertTrue(
        conformanceSubReport.get("errorMessages").isEmpty(),
        "Should be empty, but found: " + conformanceSubReport.get("errorMessages"));
  }

  void validateSandboxScenarioGroup(SandboxConfig sandbox1, String scenarioId) {
    log.info("Validating scenario group: {}", scenarioId);
    JsonNode jsonNode = getScenarioStatus(sandbox1, scenarioId);
    assertFalse(jsonNode.get("isRunning").asBoolean());
    JsonNode conformanceSubReport = jsonNode.get("conformanceSubReport");
    assertEquals("CONFORMANT", conformanceSubReport.get("status").asText());
    assertTrue(
        conformanceSubReport.get("errorMessages").isEmpty(),
        "Should be empty, but found: " + conformanceSubReport.get("errorMessages"));
  }

  JsonNode getScenarioStatus(SandboxConfig sandbox, String scenarioId) {
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "getScenarioStatus")
            .put("sandboxId", sandbox.sandboxId)
            .put("scenarioId", scenarioId);
    return app.webuiHandler.handleRequest(USER_ID, node);
  }

  void waitForAsyncCalls(long delay) {
    try {
      Thread.sleep(delay); // Wait for the scenario to finish
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  void waitForCleanSandboxStatus(SandboxConfig sandbox) {
    String sandboxStatus = "";
    int counter = 0;
    do {
      ObjectNode node =
          mapper
              .createObjectNode()
              .put("operation", "getSandboxStatus")
              .put("sandboxId", sandbox.sandboxId);
      sandboxStatus = app.webuiHandler.handleRequest(USER_ID, node).toString();
      if (sandboxStatus.contains("[]")) return;
      try {
        counter++;
        Thread.sleep(500L); // Wait for the scenario to finish
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } while (counter < 20 && !sandboxStatus.contains("{\"waiting\":[]}"));
    log.info(
        "Waited for {} ms for sandbox status to reach the expected state: {}",
        counter * 500,
        sandboxStatus);
    if (counter == 20) {
      throw new RuntimeException(
          "Sandbox status did not reach the expected state on time: " + sandboxStatus);
    }
  }

  void updateSandboxConfigBeforeStarting(SandboxConfig sandbox1, SandboxConfig sandbox2) {
    // Update Sandbox Config 1, with details from Sandbox Config 2
    JsonNode node =
        mapper
            .createObjectNode()
            .put("operation", "updateSandboxConfig")
            .put("sandboxId", sandbox1.sandboxId)
            .put("sandboxName", sandbox1.sandboxName)
            .put("externalPartyUrl", sandbox2.sandboxUrl)
            .put("externalPartyAuthHeaderName", sandbox2.sandboxAuthHeaderName)
            .put("externalPartyAuthHeaderValue", sandbox2.sandboxAuthHeaderValue);
    assertTrue(app.webuiHandler.handleRequest(USER_ID, node).isEmpty());

    // Validate Sandbox 1 details
    node =
        mapper
            .createObjectNode()
            .put("operation", "getSandbox")
            .put("sandboxId", sandbox1.sandboxId)
            .put("includeOperatorLog", true);
    JsonNode jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.has("id"));
    assertTrue(jsonNode.has("name"));
    assertTrue(jsonNode.has("operatorLog"));
    assertEquals(jsonNode.get("id").asText(), sandbox1.sandboxId);
  }

  SandboxConfig createSandbox(Sandbox sandbox) {
    JsonNode node =
        mapper
            .createObjectNode()
            .put("operation", "createSandbox")
            .put("standardName", sandbox.standardName)
            .put("versionNumber", sandbox.versionNumber)
            .put("scenarioSuite", sandbox.scenarioSuite)
            .put("testedPartyRole", sandbox.testedPartyRole)
            .put("isDefaultType", sandbox.isDefaultType)
            .put("sandboxName", sandbox.sandboxName);
    JsonNode jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.has("sandboxId"));
    String sandboxId = jsonNode.get("sandboxId").asText();

    // Get the sandbox config
    node =
        mapper.createObjectNode().put("operation", "getSandboxConfig").put("sandboxId", sandboxId);
    jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    return mapper.convertValue(jsonNode, SandboxConfig.class);
  }

  List<ScenarioDigest> getScenarioDigests(String sandboxId) {
    JsonNode node =
        mapper
            .createObjectNode()
            .put("operation", "getScenarioDigests")
            .put("sandboxId", sandboxId);
    JsonNode jsonNode = app.webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.isArray());
    return mapper.convertValue(jsonNode, new TypeReference<>() {});
  }

  record Sandbox(
      String standardName,
      String versionNumber,
      String scenarioSuite,
      String testedPartyRole,
      boolean isDefaultType,
      String sandboxName) {}

  record SandboxConfig(
      String sandboxId,
      String sandboxName,
      String sandboxUrl,
      String sandboxAuthHeaderName,
      String sandboxAuthHeaderValue,
      String externalPartyUrl,
      String externalPartyAuthHeaderName,
      String externalPartyAuthHeaderValue,
      HttpHeaderConfiguration[] externalPartyAdditionalHeaders) {}

  record ScenarioDigest(String moduleName, List<Scenario> scenarios) {}

  record Scenario(String id, String name, boolean isRunning, String conformanceStatus) {}
}
