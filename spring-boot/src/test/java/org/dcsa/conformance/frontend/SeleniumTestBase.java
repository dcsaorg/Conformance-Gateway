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

    wait = new FluentWait<>(driver)
      .withTimeout(Duration.ofSeconds(5L))
      .pollingEvery(Duration.ofMillis(100L))
      .ignoring(NoSuchElementException.class);
  }

  @AfterAll
  static void tearDown() {
    if (driver != null) {
      driver.quit();
    }
  }

  protected void createSandboxesAndRunGroups(Standard standard, String version, String suiteName, String role) {
    String readableStandardSpec = "%s, version: %s, suite: %s, role: %s".formatted(standard.name(), version, suiteName, role);
    log.info("Starting standard: {}", readableStandardSpec);
    switchToTab(0);
    SandboxConfig sandBox1 = createSandbox(standard, version, suiteName, role, 0);
    openNewTab();
    switchToTab(1);
    SandboxConfig sandBox2 = createSandbox(standard, version, suiteName, role, 1);
    updateSandboxConfigBeforeStarting(sandBox1, sandBox2);

    runScenarios(readableStandardSpec);
    log.info("Finished with standard: {}", readableStandardSpec);

    // Close tab and switch back to first tab.
    driver.close();
    driver.switchTo().window(driver.getWindowHandles().iterator().next());
  }

  void runScenarios(String name) {
    waitForUIReadiness();
    // WebElement can not be reused after the page is refreshed. Therefore, using the index to get the button.
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
        log.info("Current action: {}", driver.findElement(By.cssSelector("[testId='currentAction']")).getText());
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

    WebElement confirmResetButton =
        driver
            .findElement(By.cssSelector("app-confirmation-dialog"))
            .findElements(By.tagName("button"))
            .getFirst(); // Equivalent to YES button in the dialogue box

    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmResetButton);

    By confirmationDialog = By.cssSelector("app-confirmation-dialog");
    wait.until(ExpectedConditions.invisibilityOfElementLocated(confirmationDialog));
    waitForUIReadiness();
    waitForAsyncCalls(lambdaDelay);

    driver.findElement(By.cssSelector("[testId='refreshButton']")).click();
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

    String standardName = driver.findElement(By.cssSelector("[testId='standardName']")).getText();
    String testedPartyRole =
        driver.findElement(By.cssSelector("[testId='testedPartyRole']")).getText();
    String currentAction = driver.findElement(By.cssSelector("[testId='currentAction']")).getText();

    if (standardName.equals(EblStandard.INSTANCE.getName())) {
      if (testedPartyRole.equals(EblRole.CARRIER.getConfigName())
          && currentAction.equals(UC6_Carrier_PublishDraftTransportDocumentAction.ACTION_TITLE)) {
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
    driver.findElement(By.id("submitActionButton")).click();
    waitForUIReadiness();
    waitForAsyncCalls(lambdaDelay * 2); // Extra delay for the async calls to finish.
    return true;
  }

  private String fetchPromptAnswer(String answerFor) {
    handlePromptText();
    switchToTab(1);

    driver.findElement(By.cssSelector("[testId='refreshButton']")).click();
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

    driver.findElement(By.cssSelector("[testId='refreshButton']")).click();
    waitForUIReadiness();
    waitForAsyncCalls(lambdaDelay);

    String operatorLog = driver.findElement(By.cssSelector("[testId='operatorLog']")).getText();
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
        driver.findElement(By.id("notifyPartyButton")).click();
        log.debug("Notify party");
        waitForAsyncCalls(lambdaDelay);

        driver.findElement(By.cssSelector("[testId='refreshButton']")).click();
        waitForUIReadiness();

        switchToTab(0);
        // refresh page 1
        driver.findElement(By.id("refreshStatusButton")).click();
        waitForUIReadiness();
      }
    } catch (org.openqa.selenium.NoSuchElementException ignored) {
      // No prompt text, is fine.
    }
  }

  // If there are no more actions, the scenario is finished and should be conformant.
  private static boolean hasNoMoreActionsDisplayed(String name) {
    if (driver.findElements(By.id("nextActions")).isEmpty()
      && driver.findElements(By.cssSelector("app-text-waiting")).isEmpty()) {
      String titleValue =
        driver.findElement(By.className("conformanceStatus")).getDomProperty("title");
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
      wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("app-text-waiting")));
      log.debug("Waited for UI readiness: {}", stopWatch);
      waitForUIReadiness();
    }
  }

  private void completeAction() {
    log.debug("Completing action");
    waitForUIReadiness();

    wait.until(
      ExpectedConditions.visibilityOfElementLocated(By.id("completeCurrentActionButton")));
    driver.findElement(By.id("completeCurrentActionButton")).click();

    // Avoid ElementClickInterceptedException by using JavascriptExecutor to click the button.
    WebElement button = driver
      .findElement(By.cssSelector("app-confirmation-dialog"))
      .findElements(By.tagName("button"))
      .getFirst();
    JavascriptExecutor js = (JavascriptExecutor) driver;
    js.executeScript("arguments[0].click();", button);

    waitForUIReadiness();
  }

  private void updateSandboxConfigBeforeStarting(SandboxConfig sandbox1, SandboxConfig sandbox2) {
    log.info("Updating both sandbox configs before starting");
    // Starts in the 2nd tab
    if (sandbox1.sandboxUrl() != null) {
      driver.findElement(By.name("externalPartyUrlTextField")).sendKeys(sandbox1.sandboxUrl());
      driver.findElement(By.name("externalPartyAuthHeaderNameTextField")).sendKeys(sandbox1.sandboxAuthHeaderName());
      driver.findElement(By.name("externalPartyAuthHeaderValueTextField")).sendKeys(sandbox1.sandboxAuthHeaderValue());
      driver.findElement(By.cssSelector("[testId='updateSandboxButton']")).click();
    } else {
      driver.findElement(By.cssSelector("[testId='cancelUpdateSandboxButton']")).click();
    }
    waitForUIReadiness();
    assertTrue(driver.findElement(By.className("pageTitle")).getText().startsWith("Sandbox: "));

    // Starts in the 1st tab
    switchToTab(0);
    if (sandbox2.sandboxUrl() != null) {
      driver.findElement(By.name("externalPartyUrlTextField")).sendKeys(sandbox2.sandboxUrl());
      driver.findElement(By.name("externalPartyAuthHeaderNameTextField")).sendKeys(sandbox2.sandboxAuthHeaderName());
      driver.findElement(By.name("externalPartyAuthHeaderValueTextField")).sendKeys(sandbox2.sandboxAuthHeaderValue());
      driver.findElement(By.cssSelector("[testId='updateSandboxButton']")).click();
    } else {
      driver.findElement(By.cssSelector("[testId='cancelUpdateSandboxButton']")).click();
    }
    waitForUIReadiness();
    assertTrue(driver.findElement(By.className("pageTitle")).getText().startsWith("Sandbox: "));
  }

  protected void loginUser() {
    if (alreadyLoggedIn) return; // Already logged in, is fine.
    driver.get(baseUrl);
    waitForAsyncCalls(lambdaDelay); // First redirect might be slow
    assertEquals(baseUrl + "/login", driver.getCurrentUrl());

    WebElement textBoxEmail = driver.findElement(By.id("login_email"));
    WebElement textBoxPassword = driver.findElement(By.id("login_password"));
    textBoxEmail.sendKeys(loginEmail);
    textBoxPassword.sendKeys(loginPassword);

    log.info("Logging in user into environment: {}", baseUrl);
    WebElement submitButton = driver.findElement(By.id("login_button"));
    submitButton.click();
    waitForAsyncCalls(lambdaDelay * 3); // First login is slow in AWS, so wait a bit longer.
    waitForUIReadiness();

    assertEquals("Sandboxes", driver.findElement(By.className("pageTitle")).getText());
    assertEquals(baseUrl + "/environment", driver.getCurrentUrl());
    alreadyLoggedIn = true;
  }

  SandboxConfig createSandbox(Standard standard, String version, String suiteName, String roleName, int sandboxType) {
    loginUser();
    log.info("Creating Sandbox: {}, {}, {}, {}, type: {}", standard.name(), version, suiteName, roleName, sandboxType);
    driver.get(baseUrl + "/create-sandbox");
    assertEquals(baseUrl + "/create-sandbox", driver.getCurrentUrl());
    waitForUIReadiness();

    selectAndPickOption("standardSelect", standard.name());
    selectAndPickOption("versionSelect", version);
    selectAndPickOption("suiteSelect", suiteName);
    selectAndPickOption("roleSelect", roleName);

    driver.findElement(By.id("sandboxTypeSelect")).click();
    List<WebElement> typeOptions = driver.findElement(By.id("sandboxTypeSelect-panel"))
      .findElements(By.tagName("mat-option"));
    assertFalse(typeOptions.isEmpty());
    typeOptions.get(sandboxType).click();

    String sandboxName = getSandboxName(standard.name(), version, suiteName, roleName, sandboxType);
    driver.findElement(By.id("mat-input-0")).sendKeys(sandboxName);
    driver.findElement(By.id("createSandboxButton")).click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[testId='sandboxNameInput']")));
    boolean noSandboxUrlInput = driver.findElements(By.cssSelector("[testId='sandboxUrlInput']")).isEmpty();
    String sandboxURL = noSandboxUrlInput ? null : driver.findElement(By.cssSelector("[testId='sandboxUrlInput']")).getDomProperty("value");
    String sandboxAuthHeaderName = noSandboxUrlInput ? null : driver.findElement(By.cssSelector("[testId='sandboxAuthHeaderNameInput']")).getDomProperty("value");
    String sandboxAuthHeaderValue = noSandboxUrlInput ? null : driver.findElement(By.cssSelector("[testId='sandboxAuthHeaderValueInput']")).getDomProperty("value");
    return new SandboxConfig(
      null,
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
