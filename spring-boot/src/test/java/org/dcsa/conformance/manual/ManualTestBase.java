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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.EndpointUriOverrideConfiguration;
import org.dcsa.conformance.core.party.HttpHeaderConfiguration;
import org.dcsa.conformance.sandbox.ConformanceWebuiHandler;
import org.dcsa.conformance.springboot.ConformanceApplication;
import org.dcsa.conformance.standards.ebl.EblStandard;
import org.dcsa.conformance.standards.ebl.action.UC6_Carrier_PublishDraftTransportDocumentAction;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.eblinterop.PintStandard;
import org.dcsa.conformance.standards.eblinterop.action.ReceiverSupplyScenarioParametersAndStateSetupAction;
import org.dcsa.conformance.standards.eblinterop.action.SenderSupplyScenarioParametersAction;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;
import org.dcsa.conformance.standards.eblissuance.EblIssuanceStandard;
import org.dcsa.conformance.standards.eblissuance.action.CarrierScenarioParametersAction;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/** Base class which contains all API call methods and wiring needed to perform a manual test */
@Slf4j
public abstract class ManualTestBase {
  protected static final String USER_ID = "unit-test";

  protected final ObjectMapper mapper = OBJECT_MAPPER;
  protected long lambdaDelay = 0L;

  protected SandboxConfig sandboxOne;
  protected SandboxConfig sandboxTwo;
  protected Set<SandboxConfig> createdSandboxes = new HashSet<>();

  @Autowired protected ConformanceApplication app;
  protected ConformanceWebuiHandler webuiHandler;

  @BeforeEach
  public void setUp() {
    webuiHandler = app.getWebuiHandler();
  }

  @AfterEach
  public void reset() {
    deleteSandboxes();
  }

  protected void runManualTests(
      String standardName,
      String standardVersion,
      String suiteName,
      String roleName,
      boolean secondRun) {
    SandboxConfig sandbox1;
    SandboxConfig sandbox2;
    if (!secondRun) {
      sandbox1 =
          createSandbox(
              new Sandbox(
                  standardName,
                  standardVersion,
                  suiteName,
                  roleName,
                  true,
                  getSandboxName(standardName, standardVersion, suiteName, roleName, 0)));
      sandbox2 =
          createSandbox(
              new Sandbox(
                  standardName,
                  standardVersion,
                  suiteName,
                  roleName,
                  false,
                  getSandboxName(standardName, standardVersion, suiteName, roleName, 1)));
      updateTestedPartySandboxConfigBeforeStarting(sandbox1, sandbox2);
      updateCounterPartySandboxConfigBeforeStarting(sandbox1, sandbox2);
    } else {
      sandbox1 =
          getSandboxByName(getSandboxName(standardName, standardVersion, suiteName, roleName, 0));
      sandbox2 =
          getSandboxByName(getSandboxName(standardName, standardVersion, suiteName, roleName, 1));
      log.info("Run for the 2nd time, and verify it still works.");
      log.info(
          "Using sandboxes: {} v{}, suite: {}, role: {}",
          standardName,
          standardVersion,
          suiteName,
          roleName);
      resetSandbox(
          sandbox2); // Make sure the sandbox does not keep an optional state from the first run

      createdSandboxes.add(sandbox1);
      createdSandboxes.add(sandbox2);
    }

    List<ScenarioDigest> sandbox1Digests = getScenarioDigests(sandbox1.sandboxId());
    assertFalse(sandbox1Digests.isEmpty(), "No scenarios found!");

    List<ScenarioDigest> sandbox2Digests = getScenarioDigests(sandbox2.sandboxId());
    assertTrue(sandbox2Digests.isEmpty(), "Scenarios found!");
    getAllSandboxes();

    // Run all tests for all scenarios
    runAllTests(sandbox1Digests, sandbox1, sandbox2);
    log.info("Done with {} as role: {}", standardName, roleName);
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
    long conformantSubReportsStart = countConformantSubReports(sandbox, scenarioId);
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
    waitUntilScenarioStatusProgresses(sandbox, scenarioId, conformantSubReportsStart);
  }

  private void waitUntilScenarioStatusProgresses(SandboxConfig sandbox, String scenarioId, long conformantSubReportsStart) {
    waitForCleanSandboxStatus(sandbox);

    // STNG-210: eBL Issuance, Conformance, uses 2 input prompts, while not progressing conformance.
    if (sandbox.sandboxName.contains("eBL Issuance")
      && sandbox.sandboxName.contains("Conformance")
      && conformantSubReportsStart == 0) {
      waitForAsyncCalls(500L);
      if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 6);
      return;
    }
    // STNG-210: PINT, Conformance, uses 2 input prompts for some cases, while not progressing conformance.
    if (sandbox.sandboxName.contains("PINT")) {
      waitForAsyncCalls(500L);
      if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 6);
      return;
    }

    // Wait until the scenario has finished and is conformant
    int i = 0;
    long currentCount = countConformantSubReports(sandbox, scenarioId);
    while (conformantSubReportsStart == currentCount) {
      // Check if input is required, if so, conformance is not progressing, so continue.
      JsonNode jsonNode = getScenarioStatus(sandbox, scenarioId);
      boolean inputRequired = jsonNode.has("inputRequired") && jsonNode.get("inputRequired").asBoolean();
      if (inputRequired) {
        log.debug("Input is required (again), return.");
        return;
      }

      log.debug("Waiting for scenario to finish.");
      waitForAsyncCalls(50L);
      i++;
      if (i > 200) {
        String message =
            "Scenario did not finish in time or in the correct Conformant state. In Sandbox: "
                + sandbox.sandboxName;
        log.error(message);
        fail(message);
      }
      currentCount = countConformantSubReports(sandbox, scenarioId);
    }
  }

  private long countConformantSubReports(SandboxConfig sandbox, String scenarioId) {
    JsonNode conformanceSubReport =
        getScenarioStatus(sandbox, scenarioId).get("conformanceSubReport");
    SubReport subReport = mapper.convertValue(conformanceSubReport, SubReport.class);
    if (subReport == null || subReport.subReports == null) {
      return 0;
    }
    // If the scenario is not conformant, return -1 and don't count any sub reports.
    if (subReport.status.equals("NON_CONFORMANT")) {
      return -1;
    }
    return subReport.subReports.stream()
        .filter(subReport1 -> subReport1.status.equals("CONFORMANT"))
        .count();
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

  void notifyAction(SandboxConfig sandbox, SandboxConfig otherSandbox) {
    log.debug("Notifying party.");
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "notifyParty")
            .put("sandboxId", sandbox.sandboxId);
    assertTrue(webuiHandler.handleRequest(USER_ID, node).isEmpty());
    if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 2);
    waitForAsyncCalls(150L);
    waitForCleanSandboxStatus(sandbox);
    waitForCleanSandboxStatus(otherSandbox);
  }

  void resetParty(SandboxConfig sandbox) {
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "resetParty")
            .put("sandboxId", sandbox.sandboxId);
    waitForAsyncCalls(150L);
    assertTrue(webuiHandler.handleRequest(USER_ID, node).isEmpty());
  }

  void completeAction(SandboxConfig sandbox) {
    log.debug("Completing current action.");
    ObjectNode node =
        mapper
            .createObjectNode()
            .put("operation", "completeCurrentAction")
            .put("sandboxId", sandbox.sandboxId)
            .put("skip", false);
    JsonNode jsonNode = webuiHandler.handleRequest(USER_ID, node);
    assertTrue(jsonNode.isEmpty(), "Should be empty, found: " + jsonNode);
    waitForCleanSandboxStatus(sandbox);
    waitForAsyncCalls(50L);
    if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 4);
  }

  private void deleteSandboxes() {
    createdSandboxes.stream()
        .filter(Objects::nonNull)
        .forEach(
            sandbox -> {
              JsonNode node =
                  mapper
                      .createObjectNode()
                      .put("operation", "deleteSandbox")
                      .put("sandboxId", sandbox.sandboxId());
              webuiHandler.handleRequest(USER_ID, node);
              log.info("Deleted sandbox: {}", sandbox.sandboxName());
            });
  }

  private void validateSandboxScenarioGroup(SandboxConfig sandbox1, String scenarioId, String scenarioName) {
    waitForCleanSandboxStatus(sandbox1);
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
      String errorMessage = "Scenario '" + scenarioName + "' is not conformant. Details: " + messageBuilder;
      log.error(errorMessage);

      // Note: developers can uncomment the next line, if they like to use the WebUI, at this point
      // waitForAsyncCalls(10 * 60 * 1000L);
      fail(errorMessage);
    }

    assertTrue(
        subReport.errorMessages.isEmpty(),
        "Should be empty, but found: '" + subReport.errorMessages + "'.\n" + message);
  }

  protected void buildErrorMessage(SubReport subReport, StringBuilder messageBuilder) {
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
      waitForAsyncCalls(20L); // Wait for the scenario to finish
    } while (counter < 1000);

    log.warn(
        "Waited for {} ms for sandbox status to reach the expected state: {}",
        counter * 20,
        sandboxStatus);
    throw new RuntimeException(
          "Sandbox status did not reach the expected state on time: " + sandboxStatus);
  }

  void updateTestedPartySandboxConfigBeforeStarting(
      SandboxConfig sandbox1, SandboxConfig sandbox2) {
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

  void updateCounterPartySandboxConfigBeforeStarting(
      SandboxConfig sandbox1, SandboxConfig sandbox2) {
    updateTestedPartySandboxConfigBeforeStarting(sandbox2, sandbox1);
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
                    scenario -> {
                      try {
                        runScenario(sandbox1, sandbox2, scenario.id(), scenario.name());
                      } catch (Exception e) {
                        log.error(
                            "Failed to run scenario id='{}' name='{}'",
                            scenario.id(),
                            scenario.name());
                        throw e;
                      }
                    }));
  }

  void runScenario(
    SandboxConfig sandbox1, SandboxConfig sandbox2, String scenarioId, String scenarioName) {
    log.debug("Starting scenario '{}'.", scenarioName);
    resetParty(sandbox2);
    startOrStopScenario(sandbox1, scenarioId);
    notifyAction(sandbox2, sandbox1);

    boolean isRunning;
    do {
      JsonNode scenarioStatusJsonNode = getScenarioStatus(sandbox1, scenarioId);
      boolean inputRequired = scenarioStatusJsonNode.has("inputRequired") && scenarioStatusJsonNode.get("inputRequired").asBoolean();
      boolean hasPromptText = scenarioStatusJsonNode.has("promptText");
      isRunning = scenarioStatusJsonNode.get("isRunning").booleanValue();
      if (inputRequired) {
        SandboxItem sandbox = getSandbox(sandbox1);
        String standardName = sandbox.standardName;
        String testedPartyRole = sandbox.testedPartyRole;
        String currentAction = scenarioStatusJsonNode.get("nextActions").asText().split(" - ")[0];

        JsonNode jsonForPrompt = scenarioStatusJsonNode.get("jsonForPromptText");
        if (standardName.equals(EblStandard.INSTANCE.getName())) {
          if (testedPartyRole.equals(EblRole.CARRIER.getConfigName())
              && currentAction.startsWith(
                  UC6_Carrier_PublishDraftTransportDocumentAction.ACTION_TITLE)) {
            jsonForPrompt = fetchTransportDocument(sandbox2, sandbox1);
          }
        } else if (standardName.equals(PintStandard.INSTANCE.getName())) {
          if (testedPartyRole.equals(PintRole.SENDING_PLATFORM.getConfigName())
              && currentAction.startsWith(SenderSupplyScenarioParametersAction.ACTION_PREFIX)) {
            jsonForPrompt = fetchPromptAnswer(sandbox2, sandbox1, "supplyScenarioParameters");
          } else if (testedPartyRole.equals(PintRole.RECEIVING_PLATFORM.getConfigName())
              && currentAction.startsWith(
                  ReceiverSupplyScenarioParametersAndStateSetupAction.ACTION_PREFIX)) {
            jsonForPrompt = fetchPromptAnswer(sandbox2, sandbox1, "initiateState");
          }
        } else if (standardName.equals(EblIssuanceStandard.INSTANCE.getName())) {
          if (testedPartyRole.equals(EblIssuanceRole.CARRIER.getConfigName())
              && currentAction.equals(CarrierScenarioParametersAction.ACTION_TITLE)) {
            jsonForPrompt = fetchPromptAnswer(sandbox2, sandbox1, "CarrierScenarioParameters");
          }
        }

        String promptActionId = scenarioStatusJsonNode.get("promptActionId").textValue();
        handleActionInput(
            sandbox1,
            scenarioId,
            promptActionId,
            jsonForPrompt);
        if (lambdaDelay > 0) waitForAsyncCalls(lambdaDelay * 2);
        continue;
      }
      if (scenarioStatusJsonNode.has("jsonForPromptText")) {
        String errorMessage = "While running '" + scenarioName + "', found an unexpected jsonForPromptText, while no input is required, got text: " + scenarioStatusJsonNode.get("jsonForPromptText");
        log.error(errorMessage);
        fail(errorMessage);
      }
      if (hasPromptText && !scenarioStatusJsonNode.get("promptText").textValue().isEmpty()) {
        notifyAction(sandbox2, sandbox1);
      }
      waitForCleanSandboxStatus(sandbox2);
      if (isRunning) completeAction(sandbox1);
    } while (isRunning);
    validateSandboxScenarioGroup(sandbox1, scenarioId, scenarioName);
  }

  @SneakyThrows
  private JsonNode fetchPromptAnswer(SandboxConfig sandbox2, SandboxConfig sandbox1, String answerFor) {
    notifyAction(sandbox2, sandbox1);
    var prompt = "Prompt answer for %s:".formatted(answerFor);

    log.debug("Fetching prompt answer for {} from sandbox2", answerFor);
    var logEntry = getSandbox(sandbox2).operatorLog.stream()
        .filter(text -> text.startsWith(prompt))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No prompt found"));
    var json = logEntry.substring(prompt.length() + 1);
    return OBJECT_MAPPER.readTree(json);
  }

  private JsonNode fetchTransportDocument(SandboxConfig sandbox2, SandboxConfig sandbox1) {
    notifyAction(sandbox2, sandbox1);

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
  protected record SandboxItem(
      String id,
      String name,
      String standardName,
      String standardVersion,
      String scenarioSuite,
      String testedPartyRole,
      boolean isDefault,
      List<String> operatorLog,
      boolean canNotifyParty,
      boolean deleted) {}

  public record EndpointUriMethod(
    String endpointUri,
    String[] methods
  ) {}

  public record SandboxConfig(
    String sandboxId,
    String sandboxName,
    String sandboxUrl,
    String sandboxAuthHeaderName,
    String sandboxAuthHeaderValue,
    String externalPartyUrl,
    String externalPartyAuthHeaderName,
    String externalPartyAuthHeaderValue,
    HttpHeaderConfiguration[] externalPartyAdditionalHeaders,
    EndpointUriMethod[] sandboxEndpointUriMethods,
    EndpointUriMethod[] externalPartyEndpointUriMethods,
    EndpointUriOverrideConfiguration[] externalPartyEndpointUriOverrides,
    String outboundApiCallsSourceIpAddress) {
    
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      SandboxConfig that = (SandboxConfig) obj;
      return java.util.Objects.equals(sandboxId, that.sandboxId);
    }
    
    @Override
    public int hashCode() {
      return java.util.Objects.hash(sandboxId);
    }
  }

  record ScenarioDigest(String moduleName, List<Scenario> scenarios) {}

  record Scenario(String id, String name, boolean isRunning, String conformanceStatus) {}

  record SubReport(String title, String status, List<SubReport> subReports, List<String> errorMessages) {}

  public record Standard(String name, List<StandardVersion> versions) {}

  protected record StandardVersion(String number, List<String> suites, List<Role> roles) {}

  protected record Role(String name, boolean noNotifications) {}
}
