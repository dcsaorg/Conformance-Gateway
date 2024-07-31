package org.dcsa.conformance.springboot;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ConformanceApplicationTest {

  @Autowired
  private ConformanceApplication app;

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @ParameterizedTest
  @CsvSource({
    "booking-200-conformance-auto-all-in-one",
    "booking-200-reference-implementation-auto-all-in-one",
    "ebl-300-conformance-si-only-auto-all-in-one",
    "ebl-300-conformance-td-only-auto-all-in-one",
//    "ebl-300-reference-implementation-auto-all-in-one", // Perhaps needs fix: STNG-128
//    "eblissuance-200-conformance-auto-all-in-one", // To be removed after DT-1442
    "eblissuance-300-conformance-auto-all-in-one",
//    "eblsurrender-200-conformance-auto-all-in-one", // To be removed after DT-1442
    "eblsurrender-300-conformance-auto-all-in-one",
    "jit-120-conformance-auto-all-in-one",
    "ovs-300-conformance-auto-all-in-one",
    "pint-300-reference-implementation-auto-all-in-one",
    "tnt-220-conformance-auto-all-in-one"
  })
  void testEachSuite(final String sandboxId) throws InterruptedException {
    if (System.currentTimeMillis() > 0) {
      log.warn("All tests are DISABLED until framework issue STNG-131 is fixed");
      return;
    }
    log.info("Starting scenario suite: {}", sandboxId);
    // validate if scenario is listed
    String rootURL = restTemplate.getForObject("http://localhost:" + port + "/", String.class);
    Assumptions.assumeTrue(rootURL.contains(sandboxId), sandboxId + " not found in root URL, skipping in this branch!");

    // Start the scenario
    String value = restTemplate.getForObject("http://localhost:" + port + getAppURL(sandboxId, "reset"), String.class);
    assertEquals("{}", value);

    // Wait for the scenario to finish
    checkUntilScenariosAreReady(sandboxId);

    // Get the report and validate it. Should have 2 times "✅ CONFORMANT" in the report.
    String report = restTemplate.getForObject("http://localhost:" + port + getAppURL(sandboxId, "report"), String.class);
    int firstFound = report.indexOf("conformance</h2><details open><summary>✅ CONFORMANT");
    int secondFound = report.indexOf("conformance</h2><details open><summary>✅ CONFORMANT", firstFound + 50);
    if (firstFound == -1 || secondFound == -1) { // Report current situation for debugging
      log.error("Report current situation: {}", report);
    }
    assertTrue(firstFound > 0, "First conformance OK not found");
    assertTrue(secondFound > firstFound, "Second conformance OK not found");
  }

  private void checkUntilScenariosAreReady(String sandboxId) throws InterruptedException {
    String status;
    String previousStatus = "";
    String startStatus = restTemplate.getForObject("http://localhost:" + port + getAppURL(sandboxId, "status"), String.class);
    do {
      Thread.sleep(3_000L);
      status = restTemplate.getForObject("http://localhost:" + port + getAppURL(sandboxId, "status"), String.class);
      if (status.equals(previousStatus)) { // Detection of a stuck scenario, prevent waiting forever. Note: turn off while debugging!
        log.error("Status did not change: {}. Originally started at: {}", status, startStatus);
        break;
      }
      log.info("Current status: {}", status);
      previousStatus = status;
      if (status.length() > "{\"scenariosLeft\":0}".length()) { // More than 9 scenarios left, wait longer
        Thread.sleep(7_000L);
      }
    } while (!status.equals("{\"scenariosLeft\":0}"));
    assertEquals("{\"scenariosLeft\":0}", status, "Scenario did not finish. Original start status: " + startStatus);
    log.info("Original start status of sandboxId: {} was: {}", sandboxId, startStatus);
  }

  private String getAppURL(String scenarioID, String urlPath) {
    return "/conformance/" + app.localhostAuthUrlToken + "/sandbox/" + scenarioID + "/" + urlPath;
  }
}