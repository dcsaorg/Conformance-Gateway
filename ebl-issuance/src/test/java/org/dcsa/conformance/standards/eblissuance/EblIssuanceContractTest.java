package org.dcsa.conformance.standards.eblissuance;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.transportDocumentCarrierContentChecks;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Set;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;
import org.junit.jupiter.api.Test;

class EblIssuanceContractTest {

  @Test
  void exposesOnlyTheDocumentedVersionRolesAndEndpoints() {
    EblIssuanceStandard standard = EblIssuanceStandard.INSTANCE;

    assertEquals(Set.of("3.0.0"), standard.getScenarioSuitesByStandardVersion().keySet());
    var endpoints =
        standard
            .getEndpointUrisAndMethodsByScenarioSuiteAndRoleName()
            .get(EblIssuanceStandard.SCENARIO_SUITE_CONFORMANCE);
    assertEquals(
        Set.of("POST"), endpoints.get("Carrier").get("/v3/ebl-issuance-responses"));
    assertEquals(
        Set.of("PUT"), endpoints.get("Platform").get("/v3/ebl-issuance-requests"));
  }

  @Test
  void buildsTheDocumentedCarrierScenario() {
    var factory =
        new EblIssuanceComponentFactory(
            "eBL Issuance", "3.0.3", EblIssuanceStandard.SCENARIO_SUITE_CONFORMANCE);
    var modules =
        EblIssuanceScenarioListBuilder.createModuleScenarioListBuilders(
            factory,
            Set.of(EblIssuanceRole.CARRIER.getConfigName()),
            "Carrier under test",
            "eBL Platform under test");

    assertEquals(Set.of("Required scenario"), modules.keySet());
    var scenarios = modules.get("Required scenario").buildScenarioList(0);
    assertEquals(1, scenarios.size());
    var scenario = scenarios.getFirst();
    assertEquals("SupplyCSP [Certificate]", scenario.popNextAction().getActionTitle());
    assertEquals("Issuance request/response", scenario.popNextAction().getActionTitle());
  }

  @Test
  void buildsTheDocumentedPlatformScenario() {
    var factory =
        new EblIssuanceComponentFactory(
            "eBL Issuance", "3.0.3", EblIssuanceStandard.SCENARIO_SUITE_CONFORMANCE);
    var modules =
        EblIssuanceScenarioListBuilder.createModuleScenarioListBuilders(
            factory,
            Set.of(EblIssuanceRole.PLATFORM.getConfigName()),
            "Carrier under test",
            "eBL Platform under test");

    assertEquals(Set.of("Required scenario"), modules.keySet());
    var scenarios = modules.get("Required scenario").buildScenarioList(0);
    assertEquals(1, scenarios.size());
    var scenario = scenarios.getFirst();
    assertEquals("SupplyCSP [Document Parties]", scenario.popNextAction().getActionTitle());
    assertEquals("Issuance request/response", scenario.popNextAction().getActionTitle());
  }

  @Test
  void syntheticCarrierRequestPassesSchemaAndAllReusedTdChecks() {
    ObjectNode request =
        (ObjectNode)
            JsonToolkit.templateFileToJsonNode(
                "/standards/eblissuance/messages/eblissuance-v3.0.0-request.json",
                Map.ofEntries(
                    Map.entry("TRANSPORT_DOCUMENT_REFERENCE_PLACEHOLDER", "TDR-1"),
                    Map.entry("TRANSPORT_DOCUMENT_SUB_REFERENCE_PLACEHOLDER", "TDSR-1"),
                    Map.entry("SHIPPING_INSTRUCTION_REFERENCE_PLACEHOLDER", "SIR-1"),
                    Map.entry("BOOKING_REFERENCE_PLACEHOLDER", "CBRR-1"),
                    Map.entry("SEND_TO_PLATFORM_PLACEHOLDER", "DCSA"),
                    Map.entry("ISSUE_TO_LEGAL_NAME_PLACEHOLDER", "Issue To"),
                    Map.entry("ISSUE_TO_CODE_LIST_PROVIDER", "W3C"),
                    Map.entry("ISSUE_TO_PARTY_CODE_PLACEHOLDER", "issue-to"),
                    Map.entry("ISSUE_TO_CODE_LIST_NAME_PLACEHOLDER", "DCSA"),
                    Map.entry("SHIPPER_LEGAL_NAME_PLACEHOLDER", "Shipper"),
                    Map.entry("SHIPPER_CODE_LIST_PROVIDER", "W3C"),
                    Map.entry("SHIPPER_PARTY_CODE_PLACEHOLDER", "shipper"),
                    Map.entry("SHIPPER_CODE_LIST_NAME_PLACEHOLDER", "DCSA"),
                    Map.entry("CONSIGNEE_LEGAL_NAME_PLACEHOLDER", "Consignee"),
                    Map.entry("CONSIGNEE_CODE_LIST_PROVIDER", "W3C"),
                    Map.entry("CONSIGNEE_PARTY_CODE_PLACEHOLDER", "consignee"),
                    Map.entry("CONSIGNEE_CODE_LIST_NAME_PLACEHOLDER", "DCSA"),
                    Map.entry("ISSUING_PARTY_LEGAL_NAME_PLACEHOLDER", "Issuing Party"),
                    Map.entry("ISSUING_PARTY_CODE_LIST_PROVIDER", "W3C"),
                    Map.entry("ISSUING_PARTY_PARTY_CODE_PLACEHOLDER", "issuer"),
                    Map.entry("ISSUING_PARTY_CODE_LIST_NAME_PLACEHOLDER", "DCSA")));
    ObjectNode manifest =
        OBJECT_MAPPER
            .createObjectNode()
            .put("documentChecksum", Checksums.sha256CanonicalJson(request.path("document")))
            .put("issueToChecksum", Checksums.sha256CanonicalJson(request.path("issueTo")));
    request.put(
        "issuanceManifestSignedContent",
        PayloadSignerFactory.carrierPayloadSigner().sign(manifest.toString()));

    assertTrue(
        JsonSchemaValidator.getInstance(
            "/standards/eblissuance/schemas/EBL_ISS_v3.0.0.yaml", "IssuanceRequest")
            .validate(request)
            .isEmpty());
    var carrierChecks = transportDocumentCarrierContentChecks();
    assertTrue(
        carrierChecks.stream()
            .anyMatch(
                check ->
                    check.description()
                        .equals(
                            "`transportDocumentStatus` must equal `DRAFT`, `APPROVED`, `ISSUED`, `PENDING_SURRENDER_FOR_AMENDMENT`, `SURRENDERED_FOR_AMENDMENT`, `PENDING_SURRENDER_FOR_DELIVERY`, `SURRENDERED_FOR_DELIVERY`, or `VOIDED`.")));
    assertFalse(
        carrierChecks.stream()
            .anyMatch(
                check ->
                    check.description()
                        .equals(
                            "The JSON body has a correct transportDocumentStatus: Must be one of '[ISSUED]'")));
    carrierChecks
        .forEach(
            check -> {
              var result = check.validate(request.path("document"));
              assertTrue(
                  result.isConformant(),
                  () -> "%s: %s".formatted(check.description(), result.getErrorMessages()));
            });
  }
}



