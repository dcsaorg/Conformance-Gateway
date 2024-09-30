package org.dcsa.conformance.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

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
  @ValueSource(strings = {
    "adoption-100-conformance-auto-all-in-one",
    "booking-200-conformance-auto-all-in-one",
    "booking-200-reference-implementation-auto-all-in-one",
    "cs-100-conformance-auto-all-in-one",
    "ebl-300-conformance-si-only-auto-all-in-one",
    "ebl-300-conformance-td-only-auto-all-in-one",
    //    "ebl-300-reference-implementation-auto-all-in-one", // Works, but takes a long time to
    // run.
    "eblissuance-300-conformance-auto-all-in-one",
    "eblsurrender-300-conformance-auto-all-in-one",
    "jit-120-conformance-auto-all-in-one",
    "ovs-300-conformance-auto-all-in-one",
    "pint-300-reference-implementation-auto-all-in-one",
    "tnt-220-conformance-auto-all-in-one"
  })
  void testEachSuite(final String sandboxId) throws InterruptedException {
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
      log.error("Report situation on sandboxId {}: {}", sandboxId, report);
    }
    assertTrue(firstFound > 0, "First conformance OK not found, in sandbox: " + sandboxId);
    assertTrue(secondFound > firstFound, "Second conformance OK not found after first, in sandbox: " + sandboxId);
  }

  private void checkUntilScenariosAreReady(String sandboxId) throws InterruptedException {
    StopWatch stopWatch = StopWatch.createStarted();
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
    stopWatch.stop();
    assertEquals("{\"scenariosLeft\":0}", status, "Scenario in sandbox '" + sandboxId + "' did not finish properly! Original start status: " + startStatus);
    log.info("Done! Run took {}. Original start status of sandboxId: {} was: {}", stopWatch, sandboxId, startStatus);
  }

  private String getAppURL(String scenarioID, String urlPath) {
    return "/conformance/" + app.localhostAuthUrlToken + "/sandbox/" + scenarioID + "/" + urlPath;
  }
}
