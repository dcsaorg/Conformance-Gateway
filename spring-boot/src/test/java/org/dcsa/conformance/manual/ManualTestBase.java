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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.HttpHeaderConfiguration;
import org.dcsa.conformance.sandbox.ConformanceWebuiHandler;
import org.dcsa.conformance.springboot.ConformanceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/** Base class which contains all API call methods and wiring needed to perform a manual test */
@Slf4j
public abstract class ManualTestBase {
  private static final String USER_ID = "unit-test";

  protected final ObjectMapper mapper = OBJECT_MAPPER;
  protected long lambdaDelay = 0L;

  @Autowired protected ConformanceApplication app;
  private ConformanceWebuiHandler webuiHandler;

  @BeforeEach
  public void setUp() {
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
    return mapper.convertValue(jsonNode, new TypeReference<>() {});
  }

  void handleActionInput(
      SandboxConfig sandbox, String scenarioId, String actionId, JsonNode textInputNode) {
    log.debug("Handling action input: {}", textInputNode);
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
    log.debug("Notifying party.");
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
    log.debug("Completing current action.");
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

  protected void waitForAsyncCalls(long delay) {
    if (delay == 0) return;
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
      counter++;
      waitForAsyncCalls(300L); // Wait for the scenario to finish
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
    getSandbox(sandbox1);
  }

  SandboxItem getSandbox(SandboxConfig sandbox) {
    JsonNode node;
    node =
        mapper
            .createObjectNode()
            .put("operation", "getSandbox")
            .put("sandboxId", sandbox.sandboxId)
            .put("includeOperatorLog", true);
    JsonNode jsonNode = webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.has("id"));
    assertTrue(jsonNode.has("name"));
    assertTrue(jsonNode.has("operatorLog"));
    assertEquals(jsonNode.get("id").asText(), sandbox.sandboxId);
    return mapper.convertValue(jsonNode, SandboxItem.class);
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
    log.info("Created sandbox: {} v{}, suite: {}, role: {}, defaultType: {}", sandbox.standardName, sandbox.versionNumber, sandbox.scenarioSuite, sandbox.testedPartyRole, sandbox.isDefaultType);

    return getSandboxConfig(sandboxId);
  }

  void resetSandbox(SandboxConfig sandbox) {
    log.info("Reset state of sandbox: {}", sandbox.sandboxName);
    JsonNode node =
        mapper
            .createObjectNode()
            .put("operation", "resetParty")
            .put("sandboxId", sandbox.sandboxId);
    JsonNode jsonNode = webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.isEmpty(), "Should be empty, found: " + jsonNode);
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

  protected String getSandboxName(String standardName, String version, String suiteName, String roleName, int sandboxType) {
    String sandboxName;
    if (sandboxType == 0) {
      sandboxName = "%s v%s, %s, %s - Testing: orchestrator".formatted(standardName, version, suiteName, roleName);
    } else {
      sandboxName = "%s v%s, %s, %s - Testing: synthetic %s as tested party"
        .formatted(standardName, version, suiteName, roleName, roleName);
    }
    return sandboxName;
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
    log.debug("Starting scenario '{}'.", scenarioName);
    startOrStopScenario(sandbox1, scenarioId);
    notifyAction(sandbox2);
    waitForCleanSandboxStatus(sandbox1);

    boolean isRunning;
    do {
      JsonNode jsonNode = getScenarioStatus(sandbox1, scenarioId);
      boolean inputRequired = jsonNode.has("inputRequired") && jsonNode.get("inputRequired").asBoolean();
      boolean hasPromptText = jsonNode.has("promptText");
      isRunning = jsonNode.get("isRunning").booleanValue();
      if (inputRequired) {
        JsonNode jsonForPrompt = jsonNode.get("jsonForPromptText");
        String jsonForPromptText = jsonForPrompt.toString();
        assertTrue(
            jsonForPromptText.length() >= 25, "Prompt text was:" + jsonForPromptText.length());
        String promptActionId = jsonNode.get("promptActionId").textValue();

        // Special flow for: eBL TD-only UC6 in Carrier mode (DT-1681)
        if (jsonForPromptText.contains("Insert TDR here")) {
          jsonForPrompt = fetchTransportDocument(sandbox2);
        }

        handleActionInput(sandbox1, scenarioId, promptActionId, jsonForPrompt);
        if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 2);
        continue;
      }
      if (jsonNode.has("jsonForPromptText")) {
        log.error("While running '{}', found an unexpected jsonForPromptText, while no input is required, got text: {}", scenarioName, jsonNode.get("jsonForPromptText"));
        fail();
      }
      if (hasPromptText && !jsonNode.get("promptText").textValue().isEmpty()) {
        notifyAction(sandbox2);
      }
      if (isRunning) completeAction(sandbox1);
    } while (isRunning);
    validateSandboxScenarioGroup(sandbox1, scenarioId, scenarioName);
  }

  private JsonNode fetchTransportDocument(SandboxConfig sandbox2) {
    notifyAction(sandbox2);

    log.debug("Fetching transport document reference from sandbox2");
    String referenceText =
        getSandbox(sandbox2).operatorLog.stream()
            .filter(text -> text.contains("transport document '"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No transport document found"));
    return OBJECT_MAPPER
        .createObjectNode()
        .put("transportDocumentReference", extractTransportDocumentReference(referenceText));
  }

  protected static String extractTransportDocumentReference(String referenceTextLine) {
    String tdRefText = "transport document '";
    int startReference = referenceTextLine.indexOf(tdRefText);
    int endReference = referenceTextLine.indexOf("'", startReference + tdRefText.length());
    return referenceTextLine.substring(startReference + tdRefText.length(), endReference);
  }

  record Sandbox(
      String standardName,
      String versionNumber,
      String scenarioSuite,
      String testedPartyRole,
      boolean isDefaultType,
      String sandboxName) {}

  // Possible result of getAllSandboxes
  protected record SandboxItem(String id, String name, List<String> operatorLog, boolean canNotifyParty) {}

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

  public record Standard(String name, List<StandardVersion> versions) {}

  protected record StandardVersion(String number, List<String> suites, List<String> roles) {}
}
