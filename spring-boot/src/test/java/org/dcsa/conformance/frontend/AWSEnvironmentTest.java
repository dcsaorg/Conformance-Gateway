package org.dcsa.conformance.frontend;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Slf4j
@Tag("Integration")
class AWSEnvironmentTest extends SeleniumTestBase {

  private static final String DEV_URL = "https://dev.conformance-development-1.dcsa.org";
  private static final String DEV_USER = "aws-conformance-development-1+ci-test@dcsa.org";

  private static final String ENV_BASE_URL = "testLoginEmail";
  private static final String ENV_LOGIN_EMAIL = "testLoginEmail";
  private static final String ENV_LOGIN_PASSWORD = "testLoginPassword";

  @Override
  @BeforeEach
  public void setUp() {
    // Browsing in AWS is slow, so we need to increase the timeouts
    driver.manage().timeouts().implicitlyWait(Duration.ofMillis(WAIT_BEFORE_TIMEOUT_IN_MILLIS * 6));
    wait.withTimeout(Duration.ofSeconds(20L));
    stopAfterFirstScenarioGroup = true;
    lambdaDelay = 2_000L; // Not adding delays, but accept to wait longer sometimes.
  }

  @Test
  void testStandardOnDevEnvironment() {
    baseUrl = getRequiredProperty(ENV_BASE_URL, DEV_URL);
    loginEmail = getRequiredProperty(ENV_LOGIN_EMAIL, DEV_USER);
    loginPassword = getRequiredProperty(ENV_LOGIN_PASSWORD, null);

    StopWatch stopWatch = StopWatch.createStarted();
    String standardName = "Ebl";
    createSandboxesAndRunGroups(
        new Standard(standardName, null), "3.0.0", "Conformance TD-only", "Carrier");
    log.info("Finished AWS testing on standard: {}, time taken: {}", standardName, stopWatch);
  }

  // Prevent sandbox names from clashing with existing sandboxes in AWS, by adding a date-timestamp
  @Override
  protected String getSandboxName(
      String standardName, String version, String suiteName, String roleName, int sandboxType) {
    String dateTimeFormatted =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss "));
    return dateTimeFormatted
        + super.getSandboxName(standardName, version, suiteName, roleName, sandboxType);
  }

  private String getRequiredProperty(String environmentProperty, String defaultValue) {
    String value = System.getProperty(environmentProperty, defaultValue);
    if (value == null) {
      throw new IllegalArgumentException(
          "Environment variable '%s' must be set, in the environment variable or as a system property"
              .formatted(environmentProperty));
    }
    return value;
  }
}
