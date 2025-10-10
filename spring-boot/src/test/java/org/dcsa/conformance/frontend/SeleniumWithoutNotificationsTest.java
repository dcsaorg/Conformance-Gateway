package org.dcsa.conformance.frontend;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.dcsa.conformance.springboot.ConformanceApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@Tag("WebUI")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = ConformanceApplication.class)
class SeleniumWithoutNotificationsTest extends SeleniumTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Ebl",
        "Booking",
        "Booking + eBL",
      })
  void testStandardWithAllVersions(String standardName) {
    app.setSimulatedLambdaDelay(lambdaDelay);
    StopWatch stopWatch = StopWatch.createStarted();
    getAllSandboxes();
    List<Standard> availableStandards = getAvailableStandards();
    Standard requestedStandard =
        availableStandards.stream()
            .filter(standard -> standard.name().equals(standardName))
            .findFirst()
            .orElseThrow();
    requestedStandard
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
                                        createSandboxesAndRunGroups(
                                            requestedStandard,
                                            version.number(),
                                            suite,
                                            role.name()))));
    log.info("Finished with standard: {}, time taken: {}", standardName, stopWatch);
  }

  @Override
  protected String getSandboxName(
      String standardName, String version, String suiteName, String roleName, int sandboxType) {
    String sandboxName;
    if (sandboxType == 0) {
      sandboxName =
          "%s v%s, %s, %s - Testing: orchestrator (without notifications)"
              .formatted(standardName, version, suiteName, roleName);
    } else {
      sandboxName =
          "%s v%s, %s, %s - Testing: synthetic %s as tested party (without notifications)"
              .formatted(standardName, version, suiteName, roleName, roleName);
    }
    return sandboxName;
  }

  @Override
  protected String getTestedPartyApiUrl(SandboxConfig sandbox2) {
    return "";
  }
}
