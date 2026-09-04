package org.dcsa.conformance.end;

import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioConformanceType;
import org.dcsa.conformance.end.action.ConsumerGetEndorsementChainAction;
import org.dcsa.conformance.end.action.SupplyScenarioParametersAction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndorsementChainScenarioListBuilderTest {

  private static final EndorsementChainComponentFactory COMPONENT_FACTORY =
    new EndorsementChainComponentFactory(
      "Ebl Endorsement Chain",
      "3.0.0",
      EblEndorsementChainStandard.SCENARIO_SUITE_CONFORMANCE);

  @Test
  void scenarioPathsMatchConformanceDocumentForRequiredCases() {
    Map<String, EndorsementChainScenarioListBuilder> builders =
      EndorsementChainScenarioListBuilder.createModuleScenarioListBuilders(
        COMPONENT_FACTORY, Set.of("Provider"), "Provider", "Consumer");

    assertTrue(builders.containsKey("Provider Conformance Scenarios"));
    assertEquals(
      List.of(
        "SupplyCSP[TDR] - GET EndorsementChain",
        "SupplyCSP[TDR + TDSR] - GET EndorsementChain"),
      titles(builders.get("Provider Conformance Scenarios")));
    assertInterchangeable(builders.get("Provider Conformance Scenarios"));
    assertEquals(
      List.of(2L, 2L),
      builders.get("Provider Conformance Scenarios").buildScenarioList(0).stream()
        .map(scenario -> scenario.allActionsStream().count())
        .toList());
  }

  @Test
  void consumerScenarioModuleHasRequiredCases() {
    Map<String, EndorsementChainScenarioListBuilder> builders =
      EndorsementChainScenarioListBuilder.createModuleScenarioListBuilders(
        COMPONENT_FACTORY, Set.of("Consumer"), "Provider", "Consumer");

    assertTrue(builders.containsKey("Consumer Conformance Scenarios"));
    assertEquals(
      List.of(
        "GET EndorsementChain (TDR)",
        "GET EndorsementChain (TDR + TDSR)"),
      titles(builders.get("Consumer Conformance Scenarios")));
    assertInterchangeable(builders.get("Consumer Conformance Scenarios"));
    assertEquals(
      List.of(1L, 1L),
      builders.get("Consumer Conformance Scenarios").buildScenarioList(0).stream()
        .map(scenario -> scenario.allActionsStream().count())
        .toList());
  }

  @Test
  void allInOneContainsBothRoleModules() {
    Map<String, EndorsementChainScenarioListBuilder> builders =
      EndorsementChainScenarioListBuilder.createModuleScenarioListBuilders(
        COMPONENT_FACTORY, Set.of("Provider", "Consumer"), "Provider", "Consumer");

    assertTrue(builders.containsKey("Provider Conformance Scenarios"));
    assertTrue(builders.containsKey("Consumer Conformance Scenarios"));
    assertEquals(2, builders.size());
  }

  @Test
  void supportedVersionMatchesSyncedConformanceDocument() {
    assertEquals(
      Set.of("3.0.0"),
      EblEndorsementChainStandard.INSTANCE.getScenarioSuitesByStandardVersion().keySet());
  }

  @Test
  void rolesAndEndpointOwnershipMatchConformanceDocument() {
    assertEquals(Set.of("Consumer", "Provider"), COMPONENT_FACTORY.getRoleNames());
    assertEquals(
      Set.of("Consumer", "Provider"),
      EblEndorsementChainStandard.INSTANCE
        .getEndpointUrisAndMethodsByScenarioSuiteAndRoleName()
        .get(EblEndorsementChainStandard.SCENARIO_SUITE_CONFORMANCE)
        .keySet());
    assertEquals(
      Set.of("GET"),
      EblEndorsementChainStandard.INSTANCE
        .getEndpointUrisAndMethodsByScenarioSuiteAndRoleName()
        .get(EblEndorsementChainStandard.SCENARIO_SUITE_CONFORMANCE)
        .get("Provider")
        .get("/endorsement-chains/{transportDocumentReference}"));
  }

  @Test
  void providerSupplyActionsRequireExactlyTheDocumentedParameters() {
    List<ConformanceScenario> scenarios =
      EndorsementChainScenarioListBuilder.createModuleScenarioListBuilders(
          COMPONENT_FACTORY, Set.of("Provider"), "Provider", "Consumer")
        .get("Provider Conformance Scenarios")
        .buildScenarioList(0);

    List<SupplyScenarioParametersAction> supplyActions =
      scenarios.stream()
        .map(
          scenario ->
            scenario.allActionsStream()
              .filter(SupplyScenarioParametersAction.class::isInstance)
              .map(SupplyScenarioParametersAction.class::cast)
              .findFirst()
              .orElseThrow())
        .toList();
    assertEquals(Map.of("transportDocumentReference", true), supplyActions.get(0).getExpectedInputAttributes());
    assertEquals(
      Map.of("transportDocumentReference", true, "transportDocumentSubReference", true),
      supplyActions.get(1).getExpectedInputAttributes());
  }

  @Test
  void consumerActionsCarryTheDocumentedParametersWithoutProviderSetupAction() {
    List<ConformanceScenario> scenarios =
      EndorsementChainScenarioListBuilder.createModuleScenarioListBuilders(
          COMPONENT_FACTORY, Set.of("Consumer"), "Provider", "Consumer")
        .get("Consumer Conformance Scenarios")
        .buildScenarioList(0);

    List<ConsumerGetEndorsementChainAction> actions =
      scenarios.stream()
        .map(
          scenario ->
            scenario.allActionsStream()
              .map(ConsumerGetEndorsementChainAction.class::cast)
              .findFirst()
              .orElseThrow())
        .toList();
    assertEquals(
      Set.of("transportDocumentReference"),
      fieldNames(actions.get(0).asJsonNode().path("suppliedScenarioParameters")));
    assertEquals(
      Set.of("transportDocumentReference", "transportDocumentSubReference"),
      fieldNames(actions.get(1).asJsonNode().path("suppliedScenarioParameters")));
  }

  private static Set<String> fieldNames(com.fasterxml.jackson.databind.JsonNode node) {
    Set<String> names = new java.util.HashSet<>();
    node.fieldNames().forEachRemaining(names::add);
    return names;
  }

  private static void assertInterchangeable(EndorsementChainScenarioListBuilder builder) {
    List<ConformanceScenario> scenarios = builder.buildScenarioList(0);
    assertEquals(2, scenarios.size());
    assertTrue(
      scenarios.stream()
        .allMatch(
          scenario ->
            scenario.getConformanceType() == ScenarioConformanceType.INTERCHANGEABLE));
  }

  private static List<String> titles(EndorsementChainScenarioListBuilder builder) {
    return builder.buildScenarioList(0).stream().map(ConformanceScenario::getTitle).toList();
  }
}


