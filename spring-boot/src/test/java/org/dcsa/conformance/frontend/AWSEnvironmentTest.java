package org.dcsa.conformance.frontend;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Slf4j
@Tag("Integration")
class AWSEnvironmentTest extends SeleniumTestBase {
  private static final String ENV_BASE_URL = "TEST_BASE_URL";
  private static final String ENV_LOGIN_EMAIL = "TEST_LOGIN_EMAIL";
  private static final String ENV_LOGIN_PASSWORD = "TEST_LOGIN_PASSWORD";

  public AWSEnvironmentTest() {
    super("Conformant");
  }

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
    baseUrl = getRequiredProperty(ENV_BASE_URL);
    loginEmail = getRequiredProperty(ENV_LOGIN_EMAIL);
    loginPassword = getRequiredProperty(ENV_LOGIN_PASSWORD);

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
        LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ")); // Keep space at end
    return dateTimeFormatted
        + super.getSandboxName(standardName, version, suiteName, roleName, sandboxType);
  }

  private String getRequiredProperty(String environmentProperty) {
    String value =
        System.getenv().getOrDefault(environmentProperty, null); // Try system property first
    if (value == null) {
      // When it fails, also check the passed -Dkey=value properties
      value = System.getProperty(environmentProperty, null);
    }

    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException(
          "Variable '%s' must be set, as an environment variable or supplied with -Dkey=value parameter."
              .formatted(environmentProperty));
    }
    return value;
  }
}
