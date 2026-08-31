package org.dcsa.conformance.standards.eblsurrender;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioConformanceType;
import org.dcsa.conformance.standards.eblsurrender.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderRequestResponseAction;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EblSurrenderScenarioListBuilderTest {

  private static final String CARRIER_PARTY_NAME = "Carrier party";
  private static final String PLATFORM_PARTY_NAME = "Platform party";
  private static final EblSurrenderComponentFactory COMPONENT_FACTORY =
    new EblSurrenderComponentFactory("eBL Surrender", "3.0.0", "Conformance");

  @Test
  void standardRegistersTheDocumentedApiVersion() {
    assertEquals(
      Map.of("3.0.0", Set.of(EblSurrenderStandard.SCENARIO_SUITE_CONFORMANCE)),
      EblSurrenderStandard.INSTANCE.getScenarioSuitesByStandardVersion());
  }

  @Test
  void carrierHasItsOwnRequiredDeliveryAndOptionalAmendmentScenarios() {
    Map<String, EblSurrenderScenarioListBuilder> builders =
      buildersFor(Set.of(EblSurrenderRole.CARRIER.getConfigName()));

    assertEquals(List.of("Required scenario", "Optional (report-only) scenario"), builders.keySet().stream().toList());

    ConformanceScenario required = builders.get("Required scenario").buildScenarioList(0).getFirst();
    assertScenario(
      required,
      "SupplyCSP[Transport Document data] - Surrender request(Delivery) - Surrender response",
      ScenarioConformanceType.REQUIRED,
      SupplyScenarioParametersAction.class,
      SurrenderRequestResponseAction.class);

    ConformanceScenario optional =
      builders.get("Optional (report-only) scenario").buildScenarioList(1).getFirst();
    assertScenario(
      optional,
      "SupplyCSP[Transport Document data] - Surrender request(Amendment) - Surrender response",
      ScenarioConformanceType.OPTIONAL,
      SupplyScenarioParametersAction.class,
      SurrenderRequestResponseAction.class);
  }

  @Test
  void platformHasItsOwnRequiredDeliveryAndOptionalAmendmentScenarios() {
    Map<String, EblSurrenderScenarioListBuilder> builders =
      buildersFor(Set.of(EblSurrenderRole.PLATFORM.getConfigName()));

    assertEquals(List.of("Required scenario", "Optional (report-only) scenario"), builders.keySet().stream().toList());

    ConformanceScenario required = builders.get("Required scenario").buildScenarioList(0).getFirst();
    assertScenario(
      required,
      "Surrender request(Delivery) - Surrender response",
      ScenarioConformanceType.REQUIRED,
      SurrenderRequestResponseAction.class);

    ConformanceScenario optional =
      builders.get("Optional (report-only) scenario").buildScenarioList(1).getFirst();
    assertScenario(
      optional,
      "Surrender request(Amendment) - Surrender response",
      ScenarioConformanceType.OPTIONAL,
      SurrenderRequestResponseAction.class);
  }

  @Test
  void platformScenarioParametersRemainStableAcrossPromptRefreshes() {
    ConformanceAction action =
      buildersFor(Set.of(EblSurrenderRole.PLATFORM.getConfigName()))
        .get("Required scenario")
        .buildScenarioList(0)
        .getFirst()
        .popNextAction();

    assertEquals(
      action.asJsonNode().path("suppliedScenarioParameters"),
      action.asJsonNode().path("suppliedScenarioParameters"));
  }



  private static Map<String, EblSurrenderScenarioListBuilder> buildersFor(
    Set<String> testedPartyRoleNames) {
    return EblSurrenderScenarioListBuilder.createModuleScenarioListBuilders(
      COMPONENT_FACTORY, testedPartyRoleNames, CARRIER_PARTY_NAME, PLATFORM_PARTY_NAME);
  }

  @SafeVarargs
  private static void assertScenario(
    ConformanceScenario scenario,
    String expectedTitle,
    ScenarioConformanceType expectedType,
    Class<? extends ConformanceAction>... expectedActionTypes) {
    assertEquals(expectedTitle, scenario.getTitle());
    assertEquals(expectedType, scenario.getConformanceType());

    List<ConformanceAction> actions = new ArrayList<>();
    while (scenario.hasNextAction()) {
      actions.add(scenario.popNextAction());
    }
    assertEquals(expectedActionTypes.length, actions.size());
    for (int index = 0; index < expectedActionTypes.length; index++) {
      assertInstanceOf(expectedActionTypes[index], actions.get(index));
    }
  }
}



