package org.dcsa.conformance.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.dcsa.conformance.springboot.ConformanceApplication;
import org.dcsa.conformance.standards.ebl.EblScenarioListBuilder;
import org.dcsa.conformance.standards.ebl.EblStandard;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@Tag("WebUI")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = ConformanceApplication.class)
class SeleniumWithNotificationTest extends SeleniumTestBase {

  @Test
  void testLoginAndCreateSandboxStart() {
    loginUser();
    driver.get(baseUrl);
    wait.until(webDriver -> webDriver.findElement(By.id("create_sandbox_button")));
    assertEquals(baseUrl + "/environment", driver.getCurrentUrl());
    driver.findElement(By.id("create_sandbox_button")).click();
    waitForUIReadiness();

    assertEquals("Create sandbox", driver.findElement(By.className("pageTitle")).getText());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Adoption", // 0:29 minutes
        "AN", // 0:36 minutes
        "Booking", // 11:52 minutes
        "CS", // 11:05 minutes
        "Ebl", // 37:09 minutes
        "eBL Issuance", // 6:03 minutes
        "eBL Surrender", // 7:59 minutes
        // TODO re-enable after SD-2491
        // "JIT", // 1:14 minutes
        "OVS", // 3:34 minutes
        "PINT", // 6:10 minutes
        "TnT", // 6:20 minutes
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
                            version
                                .roles()
                                .forEach(
                                    role -> {
                                      try {
                                        createSandboxesAndRunGroups(
                                            requestedStandard,
                                            version.number(),
                                            suite,
                                            role.name());
                                      } catch (Exception e) {
                                        log.error(
                                            "Exception in standard '{}', version '{}', suite '{}', role '{}'",
                                            requestedStandard.name(),
                                            version.number(),
                                            suite,
                                            role,
                                            e);
                                        throw e;
                                      }
                                    })));
    log.info("Finished with standard: {}, time taken: {}", standardName, stopWatch);
  }

  @Test
  @Disabled("Only for debugging")
  void testOnlyOneSpecificScenario() {
    //    try {
    createSandboxesAndRunGroups(
        new Standard(EblStandard.INSTANCE.getName(), null),
        EblStandard.INSTANCE.getScenarioSuitesByStandardVersion().keySet().stream()
            .findFirst()
            .orElseThrow(),
        EblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE_TD_ONLY,
        EblRole.CARRIER.getConfigName());
    //    } catch (Exception e) {
    //      waitForAsyncCalls(5 * 60_000);
    //    }
  }

  @Test
  @Disabled("Useful for debugging a simulated cold-start AWS Lambda on local machine")
  void testStandardOnSimulatedSlowAWSEnvironment() {
    lambdaDelay = 2000L;
    app.setSimulatedLambdaDelay(lambdaDelay);
    stopAfterFirstScenarioGroup = true;
    wait.withTimeout(Duration.ofSeconds(20L));

    StopWatch stopWatch = StopWatch.createStarted();
    String standardName = "Ebl";
    createSandboxesAndRunGroups(
        new Standard(standardName, null), "3.0.0", "Conformance TD-only", "Carrier");
    log.info("Finished AWS testing on standard: {}, time taken: {}", standardName, stopWatch);
  }
}
