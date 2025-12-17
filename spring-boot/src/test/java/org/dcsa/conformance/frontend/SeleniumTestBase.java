package org.dcsa.conformance.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.dcsa.conformance.manual.ManualTestBase;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

@Slf4j
public abstract class SeleniumTestBase extends ManualTestBase {
  protected static final int WAIT_BEFORE_TIMEOUT_IN_MILLIS = 100;
  protected static WebDriver driver;
  protected static FluentWait<WebDriver> wait;
  private static boolean alreadyLoggedIn = false;

  protected String baseUrl = "http://localhost:4200";
  protected String loginEmail = "selenium-test@dcsa.org";
  protected String loginPassword = "selenium-test-fake-password";
  protected boolean stopAfterFirstScenarioGroup = false;

  @BeforeAll
  public static void setUpOnce() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--no-sandbox");
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--disable-search-engine-choice-screen");
    options.addArguments("--headless");

    driver = new ChromeDriver(options);
    driver.manage().timeouts().implicitlyWait(Duration.ofMillis(WAIT_BEFORE_TIMEOUT_IN_MILLIS));

    wait =
        new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(5L))
            .pollingEvery(Duration.ofMillis(100L))
            .ignoring(NoSuchElementException.class);
  }

  @Override
  public void cleanUp() {}

  @AfterAll
  static void tearDown() {
    if (driver != null) {
      driver.quit();
    }
    alreadyLoggedIn = false;
  }

  protected void createSandboxesAndRunGroups(
      Standard standard, String version, String suiteName, String role) {
    String readableStandardSpec =
        "%s, version: %s, suite: %s, role: %s".formatted(standard.name(), version, suiteName, role);
    log.info("Starting standard: {}", readableStandardSpec);
    switchToTab(0);
    SandboxConfig sandbox1 = createSandbox(standard, version, suiteName, role, 0);
    openNewTab();
    switchToTab(1);
    SandboxConfig sandbox2 = createSandbox(standard, version, suiteName, role, 1);
    updateSandboxConfigBeforeStarting(sandbox1, sandbox2);

    createdSandboxes.add(sandbox1);
    createdSandboxes.add(sandbox2);

    runScenarios(readableStandardSpec);
    log.info("Finished with standard: {}", readableStandardSpec);

    deleteSandbox(sandbox1, sandbox2);

    // Close tab and switch back to first tab.
    driver.close();
    driver.switchTo().window(driver.getWindowHandles().iterator().next());
  }

  void deleteSandbox(SandboxConfig sandbox1, SandboxConfig sandbox2) {
    log.info("Deleting sandboxes");
    switchToTab(0);
    driver.get(baseUrl + "/sandbox/" + sandbox1.sandboxId());
    waitForUIReadiness();
    safeClick(By.cssSelector("[testId='deleteSandboxButton']"));

    // Confirm deletion in the confirmation dialog using helper method
    safeClickConfirmationButton(By.cssSelector("app-confirmation-dialog"));

    waitForUIReadiness();
    log.info("Deleted sandbox: {}", sandbox1.sandboxName());

    switchToTab(1);
    driver.get(baseUrl + "/sandbox/" + sandbox2.sandboxId());
    waitForUIReadiness();
    safeClick(By.cssSelector("[testId='deleteSandboxButton']"));

    // Confirm deletion in the confirmation dialog using helper method
    safeClickConfirmationButton(By.cssSelector("app-confirmation-dialog"));

    waitForUIReadiness();
    log.info("Deleted sandbox: {}", sandbox2.sandboxName());
  }

  void runScenarios(String name) {
    waitForUIReadiness();
    // WebElement can not be reused after the page is refreshed. Therefore, using the index to get
    // the button.
    int scenarioCount =
        driver
            .findElement(By.tagName("app-sandbox"))
            .findElements(By.className(("scenarioActionButton")))
            .size();

    String storedBaseSandboxURL = driver.getCurrentUrl();
    assertNotNull(storedBaseSandboxURL);
    for (int scenarioIndex = 0; scenarioIndex < scenarioCount; scenarioIndex++) {
      driver.get(storedBaseSandboxURL);
      waitForUIReadiness();
      log.info(
          "Starting scenario {} of {}: {}",
          scenarioIndex + 1,
          scenarioCount,
          driver
              .findElement(By.tagName("app-sandbox"))
              .findElements(By.className(("wrappingText")))
              .get(scenarioIndex)
              .getText());
      resetInternalParty();
      driver
          .findElement(By.tagName("app-sandbox"))
          .findElements(By.className(("scenarioActionButton")))
          .get(scenarioIndex)
          .click();
      waitForUIReadiness();

      do {
        // Wait for the current action element to be present before accessing it
        WebElement currentActionElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.cssSelector("[testId='currentAction']")));
        log.info("Current action: {}", currentActionElement.getText());

        if (handleJsonPromptForText()) continue;
        handlePromptText();
        completeAction();
      } while (!hasNoMoreActionsDisplayed(name));
      if (stopAfterFirstScenarioGroup) {
        log.info("Stopping after first scenario group");
        break;
      }
    }
  }

  private void resetInternalParty() {
    log.debug("Resetting party");
    switchToTab(1);
    waitForUIReadiness();
    waitForAsyncCalls(lambdaDelay);
    By resetBtn = By.cssSelector("button[testId='resetPartyButton']");
    wait.until(ExpectedConditions.elementToBeClickable(resetBtn)).click();

    // Confirm reset in the confirmation dialog using helper method
    By confirmationDialog = By.cssSelector("app-confirmation-dialog");
    safeClickConfirmationButton(confirmationDialog);

    wait.until(ExpectedConditions.invisibilityOfElementLocated(confirmationDialog));
    waitForUIReadiness();
    waitForAsyncCalls(lambdaDelay);

    safeClick(By.cssSelector("[testId='refreshButton']"));
    waitForUIReadiness();
    waitForAsyncCalls(lambdaDelay);

    switchToTab(0);
    waitForUIReadiness();
    waitForAsyncCalls(lambdaDelay);
    log.debug("Party reset complete");
  }

  private boolean handleJsonPromptForText() {
    String jsonForPrompt;
    By jsonPromptTextSelector = By.cssSelector("[testId='jsonPromptText']");
    WebElement jsonPromptTextElement;
    try {
      jsonPromptTextElement = driver.findElement(jsonPromptTextSelector);
      jsonForPrompt = jsonPromptTextElement.getDomProperty("value");
      assertNotNull(jsonForPrompt);
    } catch (org.openqa.selenium.NoSuchElementException e) {
      log.debug("No JSON prompt text.");
      return false;
    }

    String standardName = wait.until(
        ExpectedConditions.presenceOfElementLocated(By.cssSelector("[testId='standardName']"))).getText();
    String testedPartyRole = wait.until(
        ExpectedConditions.presenceOfElementLocated(By.cssSelector("[testId='testedPartyRole']"))).getText();
    String currentAction = wait.until(
        ExpectedConditions.presenceOfElementLocated(By.cssSelector("[testId='currentAction']"))).getText();

    if (standardName.equals(EblStandard.INSTANCE.getName())) {
      if (testedPartyRole.equals(EblRole.CARRIER.getConfigName())
          && currentAction.startsWith(
              UC6_Carrier_PublishDraftTransportDocumentAction.ACTION_TITLE)) {
        jsonForPrompt = fetchTransportDocument(jsonForPrompt);
      }
    } else if (standardName.equals(PintStandard.INSTANCE.getName())) {
      if (testedPartyRole.equals(PintRole.SENDING_PLATFORM.getConfigName())
          && currentAction.startsWith(SenderSupplyScenarioParametersAction.ACTION_PREFIX)) {
        jsonForPrompt = fetchPromptAnswer("supplyScenarioParameters");
      } else if (testedPartyRole.equals(PintRole.RECEIVING_PLATFORM.getConfigName())
          && currentAction.startsWith(
              ReceiverSupplyScenarioParametersAndStateSetupAction.ACTION_PREFIX)) {
        jsonForPrompt = fetchPromptAnswer("initiateState");
      }
    } else if (standardName.equals(EblIssuanceStandard.INSTANCE.getName())) {
      if (testedPartyRole.equals(EblIssuanceRole.CARRIER.getConfigName())
          && currentAction.equals(CarrierScenarioParametersAction.ACTION_TITLE)) {
        jsonForPrompt = fetchPromptAnswer("CarrierScenarioParameters");
      }
    }

    jsonPromptTextElement.clear();
    jsonPromptTextElement.sendKeys(jsonForPrompt);
    safeClick(By.id("submitActionButton"));
    waitForUIReadiness();
    waitForAsyncCalls(lambdaDelay * 2); // Extra delay for the async calls to finish.
    return true;
  }

  private String fetchPromptAnswer(String answerFor) {
    handlePromptText();
    switchToTab(1);

    safeClick(By.cssSelector("[testId='refreshButton']"));
    waitForUIReadiness();
    waitForAsyncCalls(lambdaDelay);

    var prompt = "Prompt answer for %s:".formatted(answerFor);
    String operatorLog =
        driver.findElements(By.cssSelector("[testId='operatorLog']")).stream()
            .map(WebElement::getText)
            .filter(log -> log.contains(prompt))
            .findFirst()
            .orElseThrow();

    String foundAnswer = operatorLog.substring(prompt.length() + 1);
    switchToTab(0);
    return foundAnswer;
  }

  private String fetchTransportDocument(String promptText) {
    handlePromptText();
    switchToTab(1);

    safeClick(By.cssSelector("[testId='refreshButton']"));
    waitForUIReadiness();
    waitForAsyncCalls(lambdaDelay);

    String operatorLog = wait.until(
        ExpectedConditions.presenceOfElementLocated(By.cssSelector("[testId='operatorLog']"))).getText();
    String reference = extractTransportDocumentReference(operatorLog);
    switchToTab(0);
    return promptText.replace("Insert TDR here", reference);
  }

  private void handlePromptText() {
    try {
      WebElement promptTextElement = driver.findElement(By.cssSelector("[testId='yourTurnIcon']"));
      if (promptTextElement.isDisplayed() && promptTextElement.isEnabled()) {
        // notify tab 2.
        switchToTab(1);
        safeClick(By.id("notifyPartyButton"));
        log.debug("Notify party");
        waitForAsyncCalls(lambdaDelay);

        safeClick(By.cssSelector("[testId='refreshButton']"));
        waitForUIReadiness();

        switchToTab(0);
        // refresh page 1
        safeClick(By.id("refreshStatusButton"));
        waitForUIReadiness();
      }
    } catch (org.openqa.selenium.NoSuchElementException ignored) {
      // No prompt text, is fine.
    }
  }

  // If there are no more actions, the scenario is finished and should be conformant.
  private boolean hasNoMoreActionsDisplayed(String name) {
    if (driver.findElements(By.id("nextActions")).isEmpty()
        && driver.findElements(By.cssSelector("app-text-waiting")).isEmpty()) {
      String titleValue = wait.until(
          ExpectedConditions.presenceOfElementLocated(By.className("conformanceStatus")))
          .getDomProperty("title");
      assertEquals(
          "Conformant",
          titleValue,
          "Scenario is not conformant: '" + titleValue + "', while running: " + name);
      log.debug("Scenario is Conformant.");
      return true;
    }
    return false;
  }

  protected static void waitForUIReadiness() {
    if (!driver.findElements(By.cssSelector("app-text-waiting")).isEmpty()) {
      StopWatch stopWatch = StopWatch.createStarted();
      wait.until(
          ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("app-text-waiting")));
      log.debug("Waited for UI readiness: {}", stopWatch);
      waitForUIReadiness();
    }
  }

  private void completeAction() {
    log.debug("Completing action");
    waitForUIReadiness();

    // Use safeClick to handle the completeCurrentActionButton with retry logic
    safeClick(By.id("completeCurrentActionButton"));

    // Wait for confirmation dialog to appear and click the first button with retry logic
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(By.cssSelector("app-confirmation-dialog")));
    safeClickConfirmationButton(By.cssSelector("app-confirmation-dialog"));

    waitForUIReadiness();
  }

  private void updateSandboxConfigBeforeStarting(SandboxConfig sandbox1, SandboxConfig sandbox2) {
    log.info("Updating both sandbox configs before starting");
    // Starts in the 2nd tab
    if (sandbox1.sandboxUrl() != null) {
      wait.until(ExpectedConditions.presenceOfElementLocated(By.name("externalPartyUrlTextField")))
          .sendKeys(sandbox1.sandboxUrl());
      wait.until(ExpectedConditions.presenceOfElementLocated(By.name("externalPartyAuthHeaderNameTextField")))
          .sendKeys(sandbox1.sandboxAuthHeaderName());
      wait.until(ExpectedConditions.presenceOfElementLocated(By.name("externalPartyAuthHeaderValueTextField")))
          .sendKeys(sandbox1.sandboxAuthHeaderValue());
      safeClick(By.cssSelector("[testId='updateSandboxButton']"));
    } else {
      safeClick(By.cssSelector("[testId='cancelUpdateSandboxButton']"));
    }
    waitForUIReadiness();
    assertTrue(wait.until(ExpectedConditions.presenceOfElementLocated(By.className("pageTitle")))
        .getText().startsWith("Sandbox: "));

    // Starts in the 1st tab
    switchToTab(0);
    if (sandbox2.sandboxUrl() != null) {
      wait.until(ExpectedConditions.presenceOfElementLocated(By.name("externalPartyUrlTextField")))
          .sendKeys(getTestedPartyApiUrl(sandbox2));
      wait.until(ExpectedConditions.presenceOfElementLocated(By.name("externalPartyAuthHeaderNameTextField")))
          .sendKeys(sandbox2.sandboxAuthHeaderName());
      wait.until(ExpectedConditions.presenceOfElementLocated(By.name("externalPartyAuthHeaderValueTextField")))
          .sendKeys(sandbox2.sandboxAuthHeaderValue());
      safeClick(By.cssSelector("[testId='updateSandboxButton']"));
    } else {
      safeClick(By.cssSelector("[testId='cancelUpdateSandboxButton']"));
    }
    waitForUIReadiness();
    assertTrue(wait.until(ExpectedConditions.presenceOfElementLocated(By.className("pageTitle")))
        .getText().startsWith("Sandbox: "));
  }

  protected String getTestedPartyApiUrl(SandboxConfig sandbox2) {
    return sandbox2.sandboxUrl();
  }

  /**
   * Safely clicks an element with retry logic to handle StaleElementReferenceException. This is
   * necessary because Angular may re-render the DOM between finding and clicking an element.
   *
   * @param by The locator to find the element
   * @param retries Maximum number of retry attempts (default 3)
   */
  protected void safeClick(By by, int retries) {
    for (int i = 0; i < retries; i++) {
      try {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(by));
        element.click();
        return; // Success, exit
      } catch (org.openqa.selenium.StaleElementReferenceException e) {
        if (i == retries - 1) {
          log.error("Failed to click element {} after {} retries", by, retries);
          throw e; // Rethrow on last retry
        }
        log.warn(
            "StaleElementReferenceException caught for element {}, retrying... (attempt {}/{})",
            by,
            i + 1,
            retries);
        try {
          Thread.sleep(200); // Brief wait before retry
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /**
   * Safely clicks an element with default 3 retries.
   *
   * @param by The locator to find the element
   */
  protected void safeClick(By by) {
    safeClick(by, 3);
  }

  /**
   * Safely clicks the first button in a confirmation dialog using JavaScript executor with retry
   * logic. This is more reliable for overlay dialogs in Angular applications.
   *
   * @param dialogSelector The selector for the confirmation dialog
   * @param retries Maximum number of retry attempts
   */
  protected void safeClickConfirmationButton(By dialogSelector, int retries) {
    for (int i = 0; i < retries; i++) {
      try {
        WebElement confirmationDialog = driver.findElement(dialogSelector);
        WebElement button = confirmationDialog.findElements(By.tagName("button")).getFirst();

        // Use JavascriptExecutor to click the button - more reliable with Angular overlays
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", button);
        return; // Success, exit retry loop
      } catch (org.openqa.selenium.StaleElementReferenceException e) {
        if (i == retries - 1) {
          log.error("Failed to click confirmation button after {} retries", retries);
          throw e; // Rethrow on last retry
        }
        log.warn(
            "StaleElementReferenceException caught, retrying... (attempt {}/{})", i + 1, retries);
        try {
          Thread.sleep(200); // Brief wait before retry
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /**
   * Safely clicks the first button in a confirmation dialog with default 3 retries.
   *
   * @param dialogSelector The selector for the confirmation dialog
   */
  protected void safeClickConfirmationButton(By dialogSelector) {
    safeClickConfirmationButton(dialogSelector, 3);
  }

  protected void loginUser() {
    if (alreadyLoggedIn) return; // Already logged in, is fine.
    driver.get(baseUrl);
    waitForAsyncCalls(lambdaDelay); // First redirect might be slow
    assertEquals(baseUrl + "/login", driver.getCurrentUrl());

    WebElement textBoxEmail = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login_email")));
    WebElement textBoxPassword = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login_password")));
    textBoxEmail.sendKeys(loginEmail);
    textBoxPassword.sendKeys(loginPassword);

    log.info("Logging in user into environment: {}", baseUrl);
    WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("login_button")));
    submitButton.click();
    waitForAsyncCalls(lambdaDelay * 3); // First login is slow in AWS, so wait a bit longer.
    waitForUIReadiness();

    assertEquals("Sandboxes", wait.until(
        ExpectedConditions.presenceOfElementLocated(By.className("pageTitle"))).getText());
    assertEquals(baseUrl + "/environment", driver.getCurrentUrl());
    alreadyLoggedIn = true;
  }

  SandboxConfig createSandbox(
      Standard standard, String version, String suiteName, String roleName, int sandboxType) {
    loginUser();
    driver.get(baseUrl + "/create-sandbox");
    assertEquals(baseUrl + "/create-sandbox", driver.getCurrentUrl());
    waitForUIReadiness();

    selectAndPickOption("standardSelect", standard.name());
    selectAndPickOption("versionSelect", version);
    selectAndPickOption("suiteSelect", suiteName);
    selectAndPickOption("roleSelect", roleName);

    wait.until(ExpectedConditions.elementToBeClickable(By.id("sandboxTypeSelect"))).click();
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sandboxTypeSelect-panel")));
    List<WebElement> typeOptions =
        driver.findElement(By.id("sandboxTypeSelect-panel")).findElements(By.tagName("mat-option"));
    assertFalse(typeOptions.isEmpty());
    typeOptions.get(sandboxType).click();

    String sandboxName = getSandboxName(standard.name(), version, suiteName, roleName, sandboxType);
    log.info("Creating Sandbox: {}", sandboxName);

    wait.until(ExpectedConditions.presenceOfElementLocated(By.id("mat-input-0"))).sendKeys(sandboxName);
    safeClick(By.id("createSandboxButton"));

    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("[testId='sandboxNameInput']")));

    String currentUrl = driver.getCurrentUrl();
    assertNotNull(currentUrl);

    String sandboxId = currentUrl.substring(currentUrl.lastIndexOf('/') + 1);
    boolean noSandboxUrlInput =
        driver.findElements(By.cssSelector("[testId='sandboxUrlInput']")).isEmpty();
    String sandboxURL =
        noSandboxUrlInput
            ? null
            : driver
                .findElement(By.cssSelector("[testId='sandboxUrlInput']"))
                .getDomProperty("value");
    String sandboxAuthHeaderName =
        noSandboxUrlInput
            ? null
            : driver
                .findElement(By.cssSelector("[testId='sandboxAuthHeaderNameInput']"))
                .getDomProperty("value");
    String sandboxAuthHeaderValue =
        noSandboxUrlInput
            ? null
            : driver
                .findElement(By.cssSelector("[testId='sandboxAuthHeaderValueInput']"))
                .getDomProperty("value");
    return new SandboxConfig(
        sandboxId,
        sandboxName,
        sandboxURL,
        sandboxAuthHeaderName,
        sandboxAuthHeaderValue,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static void selectAndPickOption(String selectBoxName, String itemToUse) {
    // Click to open the dropdown
    wait.until(ExpectedConditions.elementToBeClickable(By.id(selectBoxName))).click();

    // Wait for the panel to be visible
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(selectBoxName + "-panel")));

    // Re-find the options AFTER the panel is visible to avoid stale elements
    List<WebElement> selectBoxOptions =
        driver.findElement(By.id(selectBoxName + "-panel")).findElements(By.tagName("mat-option"));
    assertFalse(selectBoxOptions.isEmpty(), "No options found for " + selectBoxName);

    // Find the matching option
    WebElement targetOption =
        selectBoxOptions.stream()
            .filter(option -> option.getText().equals(itemToUse))
            .findFirst()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Option '" + itemToUse + "' not found in " + selectBoxName));

    // Use JavaScript click to avoid ElementClickInterceptedException
    wait.until(ExpectedConditions.elementToBeClickable(targetOption));
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("arguments[0].click();", targetOption);
  }

  private void openNewTab() {
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

  private void switchToTab(int tabIndex) {
    driver.switchTo().window(driver.getWindowHandles().toArray(new String[0])[tabIndex]);
  }
}
