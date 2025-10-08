package org.dcsa.conformance.frontend;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.dcsa.conformance.springboot.ConformanceApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@Tag("WebUI")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = ConformanceApplication.class)
class SeleniumWithoutNotificationsTest extends SeleniumTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Booking", // 11:52 minutes
        "Ebl", // 37:09 minutes
        "Booking + eBL" // 10:40 minutes
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
  protected String getTestedPartyApiUrl(SandboxConfig sandbox2) {
    return "";
  }
}
