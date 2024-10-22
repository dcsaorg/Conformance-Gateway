package org.dcsa.conformance.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.dcsa.conformance.springboot.ConformanceApplication;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@Tag("WebUI")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = ConformanceApplication.class)
class SeleniumTest extends SeleniumTestBase {

  @Test
  void testLoginAndCreateSandboxStart() {
    loginUser();
    driver.get(baseUrl);
    assertEquals(baseUrl + "/environment", driver.getCurrentUrl());
    WebElement createSandbox = driver.findElement(By.id("create_sandbox_button"));
    createSandbox.click();
    waitForUIReadiness();

    assertEquals("Create sandbox", driver.findElement(By.className("pageTitle")).getText());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "Adoption", // Takes 0:29 minutes
    "Booking", // Takes 11:52 minutes
    "CS", // 11:05 minutes
    "Ebl", // 37:09 minutes
    "eBL Issuance", // 6:03 minutes
    "eBL Surrender", // 7:59 minutes
    "JIT", // 1:14 minutes
    "OVS", // 3:34 minutes
//     "PINT", // Waits until DT-1796 is fixed
    "TnT" // 6:20 minutes
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
                                    role ->
                                        createSandboxesAndRunGroups(
                                            requestedStandard, version.number(), suite, role))));
    log.info("Finished with standard: {}, time taken: {}", standardName, stopWatch);
  }

  @Test
  @Disabled("Only for debugging")
  void testOnlyOneSpecificScenario() {
//    try {
      createSandboxesAndRunGroups(new Standard("Booking", null), "2.0.0", "Conformance", "Carrier");
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
