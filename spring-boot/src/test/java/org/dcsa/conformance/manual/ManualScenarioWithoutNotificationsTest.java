package org.dcsa.conformance.manual;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.springboot.ConformanceApplication;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = ConformanceApplication.class)
class ManualScenarioWithoutNotificationsTest extends ManualTestBase {

  private static Stream<Arguments> testStandards() {
    return Stream.of(
        Arguments.of("Booking", false),
        Arguments.of("Booking", true),
        Arguments.of("Ebl", false),
        Arguments.of("Ebl", true),
        Arguments.of("Booking + eBL", false),
        Arguments.of("Booking + eBL", true));
  }

  @ParameterizedTest(name = "Standard: {0} - 2nd run: {1}")
  @MethodSource("testStandards")
  void testStandards(String standardName, boolean secondRun) {
    app.setSimulatedLambdaDelay(lambdaDelay);
    getAllSandboxes();
    List<Standard> availableStandards = getAvailableStandards();
    Standard standard1 =
        availableStandards.stream()
            .filter(standard -> standard.name().equals(standardName))
            .findFirst()
            .orElseThrow();

    standard1
        .versions()
        .forEach(
            version ->
                version.suites().stream()
                    .filter(suite -> suite.startsWith("Conformance"))
                    .forEach(
                        suite ->
                            version.roles().stream()
                                .filter(Role::noNotifications)
                                .forEach(
                                    role ->
                                        runManualTests(
                                            standard1.name(),
                                            version.number(),
                                            suite,
                                            role.name(),
                                            secondRun))));
  }

  @Override
  protected String getSandboxName(String standardName, String version, String suiteName, String roleName, int sandboxType) {
    String sandboxName;
    if (sandboxType == 0) {
      sandboxName = "%s v%s, %s, %s - Testing: orchestrator (without notifications)".formatted(standardName, version, suiteName, roleName);
    } else {
      sandboxName = "%s v%s, %s, %s - Testing: synthetic %s as tested party (without notifications)"
              .formatted(standardName, version, suiteName, roleName, roleName);
    }
    return sandboxName;
  }

  @Override
  void updateTestedPartySandboxConfigBeforeStarting(
      SandboxConfig sandbox1, SandboxConfig sandbox2) {
    JsonNode node =
        mapper
            .createObjectNode()
            .put("operation", "updateSandboxConfig")
            .put("sandboxId", sandbox1.sandboxId())
            .put("sandboxName", sandbox1.sandboxName())
            .put("externalPartyUrl", "")
            .put("externalPartyAuthHeaderName", sandbox2.sandboxAuthHeaderName())
            .put("externalPartyAuthHeaderValue", sandbox2.sandboxAuthHeaderValue());
    assertTrue(webuiHandler.handleRequest(USER_ID, node).isEmpty());

    getSandbox(sandbox1);
  }

  @Override
  void updateCounterPartySandboxConfigBeforeStarting(
      SandboxConfig sandbox1, SandboxConfig sandbox2) {
    super.updateTestedPartySandboxConfigBeforeStarting(sandbox2, sandbox1);
  }

  @Override
  protected void validateSubReport(String scenarioName, SubReport subReport) {
    // Only fail if there are error messages OR non-notification NO_TRAFFIC reports
    boolean hasErrors = !subReport.errorMessages().isEmpty();
    boolean hasNonNotificationNoTraffic = hasNonNotificationNoTrafficReports(subReport);

    if (hasErrors || hasNonNotificationNoTraffic) {
      StringBuilder messageBuilder = new StringBuilder();
      buildErrorMessage(subReport, messageBuilder);

      String errorDetails = hasErrors ? "Error messages found" : "";
      if (hasNonNotificationNoTraffic) {
        errorDetails += (hasErrors ? " | " : "") + "Non-notification NO_TRAFFIC reports found";
      }

      String errorMessage =
          "Scenario '"
              + scenarioName
              + "' failed validation. "
              + errorDetails
              + ". Details: "
              + messageBuilder;
      log.error(errorMessage);
      fail(errorMessage);
    }
  }

  private boolean hasNonNotificationNoTrafficReports(SubReport subReport) {
    if (subReport.status().equals("NO_TRAFFIC") && !subReport.title().contains("[Notification]")) {
      return true;
    }
    return subReport.subReports().stream().anyMatch(this::hasNonNotificationNoTrafficReports);
  }
}
