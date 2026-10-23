package org.dcsa.conformance.lambda;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.dcsa.conformance.core.state.MemorySortedPartitionsLockingMap;
import org.dcsa.conformance.core.state.MemorySortedPartitionsNonLockingMap;
import org.dcsa.conformance.lambda.auth.Auth0IdentityResolver;
import org.dcsa.conformance.lambda.auth.IdentityMappingRepository;
import org.dcsa.conformance.sandbox.ConformanceAccessException;
import org.dcsa.conformance.sandbox.ConformanceSandbox;
import org.dcsa.conformance.sandbox.ConformanceWebRequest;
import org.dcsa.conformance.sandbox.ConformanceWebuiHandler;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;
import org.junit.jupiter.api.Test;

class OwnershipContinuityTest {
  final ConformancePersistenceProvider persistence =
      new ConformancePersistenceProvider(
          new MemorySortedPartitionsNonLockingMap(), new MemorySortedPartitionsLockingMap());
  final ConformanceWebuiHandler handler =
      new ConformanceWebuiHandler(
          new WebuiAccessChecker(persistence), "http://localhost:8080", persistence, task -> {});

  ObjectNode request(String operation, String sandboxId) {
    return OBJECT_MAPPER
        .createObjectNode()
        .put("operation", operation)
        .put("sandboxId", sandboxId)
        .put("includeOperatorLog", false)
        .put("skip", false);
  }

  String create(String environment) {
    return handler
        ._doHandleRequest(
            environment,
            request("createSandbox", "")
                .put("sandboxName", "Identity regression")
                .put("standardName", "Booking")
                .put("versionNumber", "2.0.0")
                .put("scenarioSuite", "Conformance")
                .put("testedPartyRole", "Shipper")
                .put("isDefaultType", true))
        .path("sandboxId")
        .asText();
  }

  @Test
  void resolvedEnvironmentPreservesAllPersonalDataAndPreventsCrossUserAccess() {
    String legacy = "original-cognito-username";
    String sandboxId = create(legacy);
    var db = persistence.getNonLockingMap();
    db.setItemValue(
        "environment#" + legacy,
        "report#digest#" + sandboxId + "#old",
        OBJECT_MAPPER
            .createObjectNode()
            .put("isoTimestamp", "old")
            .put("title", "Historical report"));
    db.setItemValue(
        "environment#" + legacy,
        "report#content#" + sandboxId + "#old",
        OBJECT_MAPPER.createObjectNode().put("report", "historical content"));
    var mappings = mock(IdentityMappingRepository.class);
    when(mappings.find(anyString())).thenReturn(legacy);
    String resolved =
        new Auth0IdentityResolver(
                "https://tenant.example/",
                mappings,
                token -> fail("Existing mapping must not call userinfo"),
                email -> fail("Existing mapping must not hash email"))
            .resolve("auth0|alice", "token");

    assertEquals(1, handler._doHandleRequest(resolved, request("getAllSandboxes", "")).size());
    assertEquals(0, handler._doHandleRequest("other-user", request("getAllSandboxes", "")).size());
    assertEquals(
        "historical content",
        handler
            ._doHandleRequest(
                resolved, request("getReportContent", sandboxId).put("reportIsoTimestamp", "old"))
            .path("report")
            .asText());
    assertEquals(
        1, handler._doHandleRequest(resolved, request("getReportDigests", sandboxId)).size());

    var config =
        (ObjectNode) handler._doHandleRequest(resolved, request("getSandboxConfig", sandboxId));
    config.put("operation", "updateSandboxConfig").put("sandboxName", "Updated after migration");
    handler._doHandleRequest(resolved, config);
    assertEquals(
        "Updated after migration",
        handler
            ._doHandleRequest(resolved, request("getSandboxConfig", sandboxId))
            .path("sandboxName")
            .asText());
    assertFalse(
        handler._doHandleRequest(resolved, request("getScenarioDigests", sandboxId)).isEmpty());
    handler._doHandleRequest(
        resolved, request("createReport", sandboxId).put("reportTitle", "After migration"));
    JsonNode reports = handler._doHandleRequest(resolved, request("getReportDigests", sandboxId));
    assertEquals(2, reports.size());
    for (JsonNode report : reports)
      if (!report.path("isoTimestamp").asText().equals("old")) {
        assertNotNull(
            handler._doHandleRequest(
                resolved,
                request("getReportContent", sandboxId)
                    .put("reportIsoTimestamp", report.path("isoTimestamp").asText())));
      }

    for (String operation :
        List.of(
            "getSandboxConfig",
            "updateSandboxConfig",
            "getSandboxStatus",
            "getSandbox",
            "getScenarioDigests",
            "getScenario",
            "getScenarioStatus",
            "handleActionInput",
            "startOrStopScenario",
            "completeCurrentAction",
            "getCurrentActionExchanges",
            "notifyParty",
            "resetParty",
            "setNotificationsSuppressed",
            "createReport",
            "getReportDigests",
            "getReportContent",
            "deleteSandbox")) {
      assertThrows(
          ConformanceAccessException.class,
          () -> handler._doHandleRequest("other-user", request(operation, sandboxId)),
          operation);
    }
    handler._doHandleRequest(resolved, request("deleteSandbox", sandboxId));
    assertTrue(ConformanceSandbox.loadSandboxConfiguration(persistence, sandboxId).isDeleted());
    assertEquals(
        "historical content",
        db.getItemValue("environment#" + legacy, "report#content#" + sandboxId + "#old")
            .path("report")
            .asText());
    assertTrue(db.getPartitionValuesBySortKey("environment#auth0|alice", "").isEmpty());
  }

  @Test
  void machineApiStillRequiresSandboxKeyNotHumanAccessToken() {
    String sandboxId = create("legacy");
    var config = ConformanceSandbox.loadSandboxConfiguration(persistence, sandboxId);
    String url = "http://localhost:8080/conformance/sandbox/" + sandboxId + "/status";
    for (Map<String, List<String>> headers :
        List.of(
            Map.<String, List<String>>of(),
            Map.of("Authorization", List.of("Bearer human-token")),
            Map.of(config.getAuthHeaderName(), List.of("wrong-key")))) {
      assertEquals(
          403,
          ConformanceSandbox.handleRequest(
                  persistence,
                  new ConformanceWebRequest("GET", url, Map.of(), headers, ""),
                  task -> {})
              .statusCode());
    }
    assertEquals(
        200,
        ConformanceSandbox.handleRequest(
                persistence,
                new ConformanceWebRequest(
                    "GET",
                    url,
                    Map.of(),
                    Map.of(config.getAuthHeaderName(), List.of(config.getAuthHeaderValue())),
                    ""),
                task -> {})
            .statusCode());
  }
}
