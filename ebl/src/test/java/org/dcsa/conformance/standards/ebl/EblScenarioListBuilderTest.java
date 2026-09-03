package org.dcsa.conformance.standards.ebl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioConformanceType;
import org.dcsa.conformance.standards.ebl.action.CarrierSupplyPayloadAction;
import org.dcsa.conformance.standards.ebl.action.UC17_Shipper_SubmitTransportDocumentAmendmentAction;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.junit.jupiter.api.Test;

class EblScenarioListBuilderTest {

  private static final String CARRIER = "Carrier";
  private static final String SHIPPER = "Shipper";

  @Test
  void onlySiOnlyAndTdOnlySuitesAreExposed() {
    assertEquals(
        Set.of(
            EblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE_SI_ONLY,
            EblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE_TD_ONLY),
        EblStandard.INSTANCE.getScenarioSuitesByStandardVersion().get("3.0.0"));

    for (String disabledSuite :
        List.of(
            EblScenarioListBuilder.SCENARIO_SUITE_SI_TD_COMBINED,
            EblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE_TD_AMENDMENTS)) {
      EblComponentFactory componentFactory =
          new EblComponentFactory("Ebl", "3.0.0", disabledSuite);
      assertThrows(
          IllegalArgumentException.class,
          () ->
              EblScenarioListBuilder.createModuleScenarioListBuilders(
                  componentFactory, Set.of(CARRIER, SHIPPER), true, "3.0.0", CARRIER, SHIPPER));
    }
  }

  @Test
  void standaloneSupplyPromptsDoNotAssumeABookingFlowOrRestrictAnyTdAmendments() {
    var standaloneTdSupply =
        new CarrierSupplyPayloadAction(
            CARRIER, ScenarioType.REGULAR_SWB, "3.0.0", null, true);
    String standalonePrompt = standaloneTdSupply.getHumanReadablePrompt();
    assertFalse(standalonePrompt.contains("previous booking step"));
    assertFalse(standalonePrompt.contains("BOOKING202507041234567890123456"));

    var anyTdAmendmentSupply =
        new CarrierSupplyPayloadAction(
            CARRIER, ScenarioType.REGULAR_STRAIGHT_BL, "3.0.0", null, true, true);
    String amendmentPrompt = anyTdAmendmentSupply.getHumanReadablePrompt();
    assertTrue(amendmentPrompt.contains("Sea Waybill, Straight B/L, or Negotiable B/L"));
    assertFalse(amendmentPrompt.contains("Make sure the ebl type remains"));
    assertFalse(amendmentPrompt.contains("REGULAR_STRAIGHT_BL"));
  }

  @Test
  void carrierTdOnlyScenariosMatchDocumentation() {
    Map<String, List<ConformanceScenario>> modules = buildModules(Set.of(CARRIER));

    assertEquals(
        List.of(
            "Required Sea Waybill scenario",
            "Required Straight B/L scenario",
            "Required Negotiable B/L scenario",
            "Optional (report-only) scenarios"),
        List.copyOf(modules.keySet()));
    assertEquals(
        List.of("SupplyCSP [Sea Waybill] - UC6 - UC7 - UC8 - GET TD"),
        titles(modules.get("Required Sea Waybill scenario")));
    assertEquals(
        List.of("SupplyCSP [Straight B/L] - UC6 - UC7 - UC8 - GET TD"),
        titles(modules.get("Required Straight B/L scenario")));
    assertEquals(
        List.of("SupplyCSP [Negotiable B/L] - UC6 - UC7 - UC8 - GET TD"),
        titles(modules.get("Required Negotiable B/L scenario")));
    assertEquals(
        List.of(
            "SupplyCSP [any TD + any TD amendment] - UC17 - GET TD (amended content)",
            "SupplyCSP [any TD + any TD amendment] - UC17 - UC19 (confirm) - GET TD",
            "SupplyCSP [any TD + any TD amendment] - UC17 - UC19 (decline) - GET TD",
            "SupplyCSP [any TD + any TD amendment] - UC17 - UC18"),
        titles(modules.get("Optional (report-only) scenarios")));
    assertTrue(
        modules.get("Optional (report-only) scenarios").stream()
            .allMatch(
                scenario ->
                    scenario.getConformanceType() == ScenarioConformanceType.OPTIONAL));
  }

  @Test
  void shipperTdOnlyScenariosMatchDocumentation() {
    Map<String, List<ConformanceScenario>> modules = buildModules(Set.of(SHIPPER));

    assertEquals(
        List.of(
            "Required Sea Waybill scenario",
            "Required Straight B/L scenario",
            "Required Negotiable B/L scenario",
            "Optional (report-only) scenarios"),
        List.copyOf(modules.keySet()));
    assertEquals(
        List.of("UC6 [REGULAR_SWB] - UC7 - GET TD"),
        titles(modules.get("Required Sea Waybill scenario")));
    assertEquals(
        List.of("UC6 [REGULAR_STRAIGHT_BL] - UC7 - GET TD"),
        titles(modules.get("Required Straight B/L scenario")));
    assertEquals(
        List.of("UC6 [REGULAR_NEGOTIABLE_BL] - UC7 - GET TD"),
        titles(modules.get("Required Negotiable B/L scenario")));
    assertEquals(
        List.of(
            "UC17 - UC19 (confirm) - GET TD (amended content)",
            "UC17 - UC18"),
        titles(modules.get("Optional (report-only) scenarios")));
    assertTrue(
        modules.get("Optional (report-only) scenarios").stream()
            .allMatch(
                scenario ->
                    scenario.getConformanceType() == ScenarioConformanceType.OPTIONAL));
  }

  @Test
  void allInOneTdOnlyScenariosKeepBothRolesWithoutModuleCollisions() {
    Map<String, List<ConformanceScenario>> modules = buildModules(Set.of(SHIPPER, CARRIER));

    assertEquals(8, modules.size());
    assertTrue(modules.containsKey("Carrier: Optional (report-only) scenarios"));
    assertTrue(modules.containsKey("Shipper: Optional (report-only) scenarios"));
    assertEquals(
        4, modules.get("Carrier: Optional (report-only) scenarios").size());
    assertEquals(
        2, modules.get("Shipper: Optional (report-only) scenarios").size());
    List<String> allTitles = modules.values().stream().flatMap(List::stream).map(ConformanceScenario::getTitle).toList();
    assertEquals(allTitles.size(), Set.copyOf(allTitles).size());
  }

  @Test
  void standaloneShipperUc17SuppliesMatchingAmendmentDataAfterReset() {
    var action =
        new UC17_Shipper_SubmitTransportDocumentAmendmentAction(
            CARRIER, SHIPPER, null, null, null, false, "3.0.0");

    var firstPrompt = action.asJsonNode();
    String tdr = firstPrompt.required("tdr").asText();
    assertTrue(!tdr.isBlank());
    assertEquals("REGULAR_STRAIGHT_BL", firstPrompt.required("scenarioType").asText());
    assertEquals(
        tdr,
        firstPrompt
            .required("amendedTransportDocument")
            .required("transportDocumentReference")
            .asText());

    action.reset();

    var resetPrompt = action.asJsonNode();
    assertEquals(tdr, resetPrompt.required("tdr").asText());
    assertEquals(
        tdr,
        resetPrompt
            .required("amendedTransportDocument")
            .required("transportDocumentReference")
            .asText());

    ObjectNode staleState = action.exportJsonState();
    ((ObjectNode) staleState.required("currentDsp"))
        .put("transportDocumentReference", "STALE-TRANSPORT-DOCUMENT-REFERENCE");
    action.importJsonState(staleState);

    var restoredPrompt = action.asJsonNode();
    assertEquals(tdr, restoredPrompt.required("tdr").asText());
    assertEquals(
        tdr,
        restoredPrompt
            .required("amendedTransportDocument")
            .required("transportDocumentReference")
            .asText());
  }

  private Map<String, List<ConformanceScenario>> buildModules(Set<String> roles) {
    EblComponentFactory componentFactory =
        new EblComponentFactory(
            "Ebl", "3.0.0", EblScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE_TD_ONLY);
    LinkedHashMap<String, EblScenarioListBuilder> builders =
        EblScenarioListBuilder.createModuleScenarioListBuilders(
            componentFactory, roles, true, "3.0.0", CARRIER, SHIPPER);
    AtomicLong moduleIndex = new AtomicLong();
    return builders.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().buildScenarioList(moduleIndex.getAndIncrement()),
                (first, ignored) -> first,
                LinkedHashMap::new));
  }

  private List<String> titles(List<ConformanceScenario> scenarios) {
    return scenarios.stream().map(ConformanceScenario::getTitle).toList();
  }
}


