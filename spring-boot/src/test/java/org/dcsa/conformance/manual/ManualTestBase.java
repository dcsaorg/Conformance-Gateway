package org.dcsa.conformance.manual;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.HttpHeaderConfiguration;
import org.dcsa.conformance.sandbox.ConformanceWebuiHandler;
import org.dcsa.conformance.springboot.ConformanceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Base class which contains all API call methods and wiring needed to perform a manual test */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = ConformanceApplication.class)
public abstract class ManualTestBase {
  private static final String USER_ID = "unit-test";

  protected final ObjectMapper mapper = OBJECT_MAPPER;
  protected long lambdaDelay = 0L;

  @Autowired protected ConformanceApplication app;
  private ConformanceWebuiHandler webuiHandler;

  @BeforeEach
  void setUp() {
    webuiHandler = app.getWebuiHandler();
  }

  protected List<Standard> getAvailableStandards() {
    ObjectNode node = mapper.createObjectNode().put("operation", "getAvailableStandards");
    JsonNode jsonNode = webuiHandler.handleRequest(USER_ID, node);
    List<Standard> standards = mapper.convertValue(jsonNode, new TypeReference<>() {});
    assertTrue(standards.size() >= 6);
    return standards;
  }

  protected List<SandboxItem> getAllSandboxes() {
    ObjectNode node = mapper.createObjectNode().put("operation", "getAllSandboxes");
    JsonNode jsonNode = webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.size() >= 6);
    // Workaround because of inconsistent data is returned.
    List<SandboxItem> sandboxItems = new ArrayList<>();
    jsonNode.forEach(
        jsonNode1 -> {
          if (jsonNode1.has("operatorLog")) {
            sandboxItems.add(
                new SandboxItem(
                    jsonNode1.get("id").asText(),
                    jsonNode1.get("name").asText(),
                    jsonNode1.get("operatorLog").asText(),
                    jsonNode1.get("canNotifyParty").asBoolean()));
          } else {
            sandboxItems.add(
                new SandboxItem(
                    jsonNode1.get("id").asText(), jsonNode1.get("name").asText(), null, false));
          }
        });
    return sandboxItems;
  }

  void handleActionInput(
      SandboxConfig sandbox, String scenarioId, String actionId, JsonNode textInputNode) {
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "handleActionInput")
            .put("sandboxId", sandbox.sandboxId)
            .put("scenarioId", scenarioId)
            .put("actionId", actionId)
            .set("actionInput", textInputNode);
    JsonNode jsonNode = webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.isEmpty(), "Should be empty, found: " + jsonNode);
    waitForCleanSandboxStatus(sandbox);
  }

  void startOrStopScenario(SandboxConfig sandbox, String scenarioId) {
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "startOrStopScenario")
            .put("sandboxId", sandbox.sandboxId)
            .put("scenarioId", scenarioId);
    assertTrue(webuiHandler.handleRequest(USER_ID, node).isEmpty());
  }

  void notifyAction(SandboxConfig sandbox) {
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "notifyParty")
            .put("sandboxId", sandbox.sandboxId);
    assertTrue(webuiHandler.handleRequest(USER_ID, node).isEmpty());
    waitForCleanSandboxStatus(sandbox);
    waitForAsyncCalls(250L);
  }

  void completeAction(SandboxConfig sandbox) {
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "completeCurrentAction")
            .put("sandboxId", sandbox.sandboxId);
    JsonNode jsonNode = webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.isEmpty(), "Should be empty, found: " + jsonNode);
    waitForCleanSandboxStatus(sandbox);
  }

  void validateSandboxScenarioGroup(SandboxConfig sandbox1, String scenarioId, String scenarioName) {
    log.info("Validating scenario '{}'.", scenarioName);
    JsonNode jsonNode = getScenarioStatus(sandbox1, scenarioId);
    assertTrue(jsonNode.has("isRunning"), "Did scenarioId '" + scenarioId + "' run? Can't find it's state. ");

    JsonNode conformanceSubReport = jsonNode.get("conformanceSubReport");
    SubReport subReport = mapper.convertValue(conformanceSubReport, SubReport.class);
    String message = "Found in scenarioId: " + scenarioId + " having '" + subReport.title + "'.";
    assertFalse(jsonNode.get("isRunning").asBoolean(), message);
    if (!subReport.status.equals("CONFORMANT")) {
      StringBuilder messageBuilder = new StringBuilder();
      buildErrorMessage(subReport, messageBuilder);
      log.error("Scenario '{}' is not conformant. Details: {}", scenarioName, messageBuilder);

      // Note: developers can uncomment the next line, if they like to use the WebUI, at this point
      // waitForAsyncCalls(10 * 60 * 1000L);
      fail();
    }
    assertTrue(
        subReport.errorMessages.isEmpty(),
        "Should be empty, but found: '"
            + subReport.errorMessages
            + "'.\n"
            + message);
  }

  private void buildErrorMessage(SubReport subReport, StringBuilder messageBuilder) {
    if (subReport.status.equals("CONFORMANT")) {
      return;
    }
    messageBuilder.append(subReport.title).append(": ").append(subReport.status).append("\n");
    for (String errorMessage : subReport.errorMessages) {
      messageBuilder.append(" - Error message: ").append(errorMessage).append("\n");
    }
    for (SubReport subReport1 : subReport.subReports) {
      buildErrorMessage(subReport1, messageBuilder);
    }
  }

  JsonNode getScenarioStatus(SandboxConfig sandbox, String scenarioId) {
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "getScenarioStatus")
            .put("sandboxId", sandbox.sandboxId)
            .put("scenarioId", scenarioId);
    return webuiHandler.handleRequest(USER_ID, node);
  }

  void waitForAsyncCalls(long delay) {
    try {
      Thread.sleep(delay); // Wait for the scenario to finish
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  void waitForCleanSandboxStatus(SandboxConfig sandbox) {
    String sandboxStatus;
    int counter = 0;
    do {
      ObjectNode node =
          mapper
              .createObjectNode()
              .put("operation", "getSandboxStatus")
              .put("sandboxId", sandbox.sandboxId);
      sandboxStatus = webuiHandler.handleRequest(USER_ID, node).toString();
      if (sandboxStatus.contains("[]")) return;
      try {
        counter++;
        Thread.sleep(300L); // Wait for the scenario to finish
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } while (counter < 30);

    log.warn(
        "Waited for {} ms for sandbox status to reach the expected state: {}",
        counter * 300,
        sandboxStatus);
    throw new RuntimeException(
          "Sandbox status did not reach the expected state on time: " + sandboxStatus);
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
    assertTrue(webuiHandler.handleRequest(USER_ID, node).isEmpty());

    // Validate Sandbox 1 details
    node =
        mapper
            .createObjectNode()
            .put("operation", "getSandbox")
            .put("sandboxId", sandbox1.sandboxId)
            .put("includeOperatorLog", true);
    JsonNode jsonNode = webuiHandler.handleRequest(USER_ID, node);
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
    JsonNode jsonNode = webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.has("sandboxId"), "SandboxId not found, maybe not created? Response: " + jsonNode);
    String sandboxId = jsonNode.get("sandboxId").asText();
    log.info("Created sandbox: {}, v{}, suite: {}, role: {}, defaultType: {}", sandbox.standardName, sandbox.versionNumber, sandbox.scenarioSuite, sandbox.testedPartyRole, sandbox.isDefaultType);

    return getSandboxConfig(sandboxId);
  }

  SandboxConfig getSandboxByName(String sandboxName) {
    SandboxItem sandboxItem1 =
        getAllSandboxes().stream()
            .filter(sandboxItem -> sandboxItem.name().equals(sandboxName))
            .findFirst()
            .orElse(null);
    if (sandboxItem1 == null) {
      return null;
    }
    return getSandboxConfig(sandboxItem1.id());
  }

  SandboxConfig getSandboxConfig(String sandboxId) {
    JsonNode node;
    JsonNode jsonNode;
    // Get the sandbox config
    node = mapper.createObjectNode().put("operation", "getSandboxConfig").put("sandboxId", sandboxId);
    jsonNode = webuiHandler.handleRequest(USER_ID, node);
    return mapper.convertValue(jsonNode, SandboxConfig.class);
  }

  List<ScenarioDigest> getScenarioDigests(String sandboxId) {
    JsonNode node =
        mapper
            .createObjectNode()
            .put("operation", "getScenarioDigests")
            .put("sandboxId", sandboxId);
    JsonNode jsonNode = webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.isArray());
    return mapper.convertValue(jsonNode, new TypeReference<>() {});
  }

  void runAllTests(
    List<ScenarioDigest> sandbox1Digests, SandboxConfig sandbox1, SandboxConfig sandbox2) {
    sandbox1Digests.forEach(
      scenarioDigest ->
        scenarioDigest
          .scenarios()
          .forEach(
            scenario -> runScenario(sandbox1, sandbox2, scenario.id(), scenario.name())));
  }

  void runScenario(
    SandboxConfig sandbox1, SandboxConfig sandbox2, String scenarioId, String scenarioName) {

    startOrStopScenario(sandbox1, scenarioId);
    notifyAction(sandbox2);

    boolean isRunning;
    do {
      JsonNode jsonNode = getScenarioStatus(sandbox1, scenarioId);
      boolean inputRequired = jsonNode.has("inputRequired") && jsonNode.get("inputRequired").asBoolean();
      boolean hasPromptText = jsonNode.has("promptText");
      isRunning = jsonNode.get("isRunning").booleanValue();
      if (inputRequired) {
        String jsonForPromptText = jsonNode.get("jsonForPromptText").toString();
        assertTrue(
          jsonForPromptText.length() >= 25, "Prompt text was:" + jsonForPromptText.length());
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

  record Sandbox(
      String standardName,
      String versionNumber,
      String scenarioSuite,
      String testedPartyRole,
      boolean isDefaultType,
      String sandboxName) {}

  // Possible result of getAllSandboxes
  protected record SandboxItem(String id, String name, String operatorLog, boolean canNotifyParty) {}

  public record SandboxConfig(
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

  record SubReport(String title, String status, List<SubReport> subReports, List<String> errorMessages) {}

  protected record Standard(String name, List<StandardVersion> versions) {}

  protected record StandardVersion(String number, List<String> suites, List<String> roles) {}
}
