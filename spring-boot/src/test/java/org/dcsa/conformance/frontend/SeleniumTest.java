package org.dcsa.conformance.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.dcsa.conformance.manual.ManualTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

@Slf4j
@Tag("WebUI")
class SeleniumTest extends ManualTestBase {

  public static final String BASE_URL = "http://localhost:4200";
  private static final int WAIT_BEFORE_TIMEOUT_IN_MILLIS = 100;
  private static WebDriver driver;
  private static FluentWait<WebDriver> wait;
  private static boolean alreadyLoggedIn = false;

  @BeforeAll
  static void setUp() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--no-sandbox");
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--disable-search-engine-choice-screen");
    options.addArguments("--headless");

    driver = new ChromeDriver(options);
    driver.manage().timeouts().implicitlyWait(Duration.ofMillis(WAIT_BEFORE_TIMEOUT_IN_MILLIS));

    wait = new FluentWait<>(driver)
      .withTimeout(Duration.ofSeconds(5))
      .pollingEvery(Duration.ofMillis(100))
      .ignoring(NoSuchElementException.class);
  }

  @AfterAll
  static void tearDown() {
    if (driver != null) {
      driver.quit();
    }
  }

  @Test
  void testLoginAndCreateSandboxStart() {
    driver.get(BASE_URL);
    if (driver.getCurrentUrl().endsWith("/login")) {
      loginUser();
    }
    assertEquals(BASE_URL + "/environment", driver.getCurrentUrl());
    WebElement createSandbox = driver.findElement(By.id("create_sandbox_button"));
    createSandbox.click();
    waitForUIReadiness();

    assertEquals("Create sandbox", driver.findElement(By.className("pageTitle")).getText());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "Booking", // Takes 10:52 minutes
    "CS", // 10:37 minutes
    "JIT", // 1:12 minutes
    "OVS", // 3:28 minutes
    "TnT" // 6:6 minutes
  })
  void testStandardWithAllVersions(String standardName) {
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
            version -> version
                .roles()
                .forEach(
                    role -> createSandboxesAndRunGroups(requestedStandard, version, "Conformance", role)));
    log.info("Finished with standard: {}, time taken: {}", standardName, stopWatch);
  }

  // Note: this method can be deleted when 'Conformance TD-only' is working. Standard can be added to method above.
  // Takes 20 minutes
  @ParameterizedTest
  @ValueSource(strings = {"Carrier", "Shipper"})
  void testEBLSIOnly(String testedParty) {
    getAllSandboxes();
    List<Standard> availableStandards = getAvailableStandards();
    Standard requestedStandard =
      availableStandards.stream()
        .filter(standard -> standard.name().equals("Ebl"))
        .findFirst()
        .orElseThrow();
    StandardVersion version = requestedStandard.versions().getFirst();
    createSandboxesAndRunGroups(requestedStandard, version, "Conformance SI-only", testedParty);
  }

  private void createSandboxesAndRunGroups(Standard standard, StandardVersion version, String suiteName, String role) {
    switchToTab(0);
    SandboxConfig sandBox1 = createSandBox(standard, version, suiteName, role, 0);
    openNewTab();
    switchToTab(1);
    SandboxConfig sandBox2 = createSandBox(standard, version, suiteName, role, 1);
    updateSandboxConfigBeforeStarting(sandBox1, sandBox2);

    runScenarioGroups();
    log.info("Finished with standard: {}, version: {}, role: {}", standard.name(), version.number(), role);

    // Close tab and switch back to first tab.
    driver.close();
    driver.switchTo().window(driver.getWindowHandles().iterator().next());
  }

  private void runScenarioGroups() {
    waitForUIReadiness();
    // WebElement can not be reused after the page is refreshed. Therefore, using the index to get the button.
    int scenarioGroupButtonIndex =
      driver
        .findElement(By.tagName("app-sandbox"))
        .findElements(By.className(("scenarioActionButton")))
        .size();

    String storedBaseSandboxURL = driver.getCurrentUrl();
    for (int i = 0; i < scenarioGroupButtonIndex; i++) {
      log.info("Starting scenario group {} of {}", i + 1, scenarioGroupButtonIndex);
      driver.get(storedBaseSandboxURL);
      driver
          .findElement(By.tagName("app-sandbox"))
          .findElements(By.className(("scenarioActionButton")))
          .get(i)
          .click();

      handleJsonPromptForText();
      do {
        handlePromptText();
        completeAction();
      } while (!hasNoMoreActionsDisplayed());
    }
  }

  private static void handleJsonPromptForText() {
    waitForUIReadiness();
    try {
      driver.findElement(By.id("jsonForPromptText"));

      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("jsonForPromptText")));
      String promptText = driver.findElement(By.id("jsonForPromptText")).getText();
      driver.findElement(By.id("actionInput")).sendKeys(promptText);
      driver.findElement(By.id("submitActionButton")).click();
      waitForUIReadiness();
    } catch (org.openqa.selenium.NoSuchElementException e) {
      log.info("No prompt text");
    }
  }

  // If there are no more actions, the scenario is finished and should be conformant.
  private static boolean hasNoMoreActionsDisplayed() {
    if (driver.findElements(By.id("nextActions")).isEmpty()
        && driver.findElements(By.tagName("app-text-waiting")).isEmpty()) {
      String titleValue =
          driver.findElement(By.className("conformanceStatus")).getAttribute("title");
      assertEquals("Conformant", titleValue);
      log.info("Scenario is Conformant.");
      return true;
    }
    return false;
  }

  private void handlePromptText() {
    try {
      WebElement promptTextElement = driver.findElement(By.id("promptText"));
      if (promptTextElement.isDisplayed() && promptTextElement.isEnabled()) {
        // notify tab 2.
        switchToTab(1);
        driver.findElement(By.id("notifyPartyButton")).click();
        log.debug("Notify party");
        switchToTab(0);
        // refresh page 1
        driver.findElement(By.id("refreshStatusButton")).click();
        waitForUIReadiness();
      }
    } catch (org.openqa.selenium.NoSuchElementException ignored) {
      // No prompt text, is fine.
    }
  }

  private static void waitForUIReadiness() {
    if (!driver.findElements(By.tagName("app-text-waiting")).isEmpty()) {
      wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("app-text-waiting")));
      waitForUIReadiness();
    }
  }

  private static void completeAction() {
    log.debug("Completing action");
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(By.id("completeCurrentActionButton")));
    driver.findElement(By.id("completeCurrentActionButton")).click();

    // Avoid ElementClickInterceptedException by using JavascriptExecutor to click the button.
    WebElement button = driver
      .findElement(By.tagName("app-confirmation-dialog"))
      .findElements(By.tagName("button"))
      .getFirst();
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("arguments[0].click();", button);

    waitForUIReadiness();
  }

  private void updateSandboxConfigBeforeStarting(SandboxConfig sandbox1, SandboxConfig sandbox2) {
    log.info("Updating both sandbox configs before starting");
    // Starts in the 2nd tab
    driver.findElement(By.name("externalPartyUrlTextField")).sendKeys(sandbox1.sandboxUrl());
    driver.findElement(By.name("externalPartyAuthHeaderNameTextField")).sendKeys(sandbox1.sandboxAuthHeaderName());
    driver.findElement(By.name("externalPartyAuthHeaderValueTextField")).sendKeys(sandbox1.sandboxAuthHeaderValue());
    driver.findElement(By.id("updateSandboxButton")).click();
    assertTrue(driver.findElement(By.className("pageTitle")).getText().startsWith("Sandbox: "));

    // Starts in the 1st tab
    switchToTab(0);
    driver.findElement(By.name("externalPartyUrlTextField")).sendKeys(sandbox2.sandboxUrl());
    driver.findElement(By.name("externalPartyAuthHeaderNameTextField")).sendKeys(sandbox2.sandboxAuthHeaderName());
    driver.findElement(By.name("externalPartyAuthHeaderValueTextField")).sendKeys(sandbox2.sandboxAuthHeaderValue());
    driver.findElement(By.id("updateSandboxButton")).click();
    assertTrue(driver.findElement(By.className("pageTitle")).getText().startsWith("Sandbox: "));
  }

  private static void loginUser() {
    assumeFalse(alreadyLoggedIn, "Already logged in, is fine.");
    driver.get(BASE_URL);
    assertEquals(BASE_URL + "/login", driver.getCurrentUrl());
    assertEquals("DCSA Conformance \uD83D\uDCBB", driver.getTitle());

    WebElement textBoxEmail = driver.findElement(By.id("login_email"));
    WebElement textBoxPassword = driver.findElement(By.id("login_password"));
    textBoxEmail.sendKeys("selenium-test@dcsa.org");
    textBoxPassword.sendKeys("selenium-test-fake-password");

    WebElement submitButton = driver.findElement(By.id("login_button"));
    submitButton.click();

    assertEquals("Sandboxes", driver.findElement(By.className("pageTitle")).getText());
    assertEquals(BASE_URL + "/environment", driver.getCurrentUrl());
    alreadyLoggedIn = true;
  }

  SandboxConfig createSandBox(Standard standard, StandardVersion version, String suiteName, String roleName, int sandboxType) {
    log.info("Creating Sandbox: {}, {}, {}, type: {}", standard.name(), version.number(), roleName, sandboxType);
    driver.get(BASE_URL + "/create-sandbox");
    if (driver.getCurrentUrl().endsWith("/login")) {
      loginUser();
      driver.get(BASE_URL + "/create-sandbox");
    }
    assertEquals(BASE_URL + "/create-sandbox", driver.getCurrentUrl());

    selectAndPickOption("standardSelect", standard.name());
    selectAndPickOption("versionSelect", version.number());
    selectAndPickOption("suiteSelect", suiteName);
    selectAndPickOption("roleSelect", roleName);

    driver.findElement(By.id("sandboxTypeSelect")).click();
    List<WebElement> typeOptions = driver.findElement(By.id("sandboxTypeSelect-panel"))
      .findElements(By.tagName("mat-option"));
    assertFalse(typeOptions.isEmpty());
    typeOptions.get(sandboxType).click();

    String sandboxName;
    if (sandboxType == 0) {
      sandboxName = "%s - %s testing: orchestrator".formatted(standard.name(), roleName);
    } else {
      sandboxName = "%s - %s testing: synthetic %s as tested party"
                  .formatted(standard.name(), roleName, roleName);
    }
    driver.findElement(By.id("mat-input-0")).sendKeys(sandboxName);
    driver.findElement(By.id("createSandboxButton")).click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sandboxUrlInput")));
    String sandboxURL = driver.findElement(By.id("sandboxUrlInput")).getAttribute("value");
    String sandboxAuthHeaderName = driver.findElement(By.id("sandboxAuthHeaderNameInput")).getAttribute("value");
    String sandboxAuthHeaderValue = driver.findElement(By.id("sandboxAuthHeaderValueInput")).getAttribute("value");
    return new SandboxConfig(
        null,
        sandboxName,
        sandboxURL,
        sandboxAuthHeaderName,
        sandboxAuthHeaderValue,
        null,
        null,
        null,
        null);
  }

  private static void selectAndPickOption(String selectBoxName, String itemToUse) {
    driver.findElement(By.id(selectBoxName)).click();
    List<WebElement> selectBoxOptions = driver.findElement(By.id(selectBoxName + "-panel"))
      .findElements(By.tagName("mat-option"));
    assertFalse(selectBoxOptions.isEmpty());
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(selectBoxName + "-panel")));
    selectBoxOptions.stream()
        .filter(option -> option.getText().equals(itemToUse))
        .findFirst()
        .orElseThrow()
        .click();
  }

  private void openNewTab(){
    ((JavascriptExecutor) driver).executeScript("window.open();");

    Set<String> windows = driver.getWindowHandles();
    // Switch to the new tab
    for (String window : windows) {
      if (!window.equals(driver.getWindowHandle())) {
        driver.switchTo().window(window);
        break;
      }
    }
  }

  private void switchToTab(int tabIndex){
    driver.switchTo().window(driver.getWindowHandles().toArray(new String[0])[tabIndex]);
  }

}
