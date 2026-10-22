package org.dcsa.conformance.standards.ovs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioConformanceType;
import org.dcsa.conformance.standards.ovs.party.OvsRole;
import org.junit.jupiter.api.Test;

class OvsScenarioListBuilderTest {

  private static final OvsComponentFactory COMPONENT_FACTORY =
      new OvsComponentFactory("OVS", "3.0.0", "Conformance");
  private static final List<String> PRODUCER_MODULES =
      List.of(
          "GET scenarios for supported filtering combinations - Alternative required path",
          "GET scenario for pagination - Optional/report-only");
  private static final List<String> CONSUMER_MODULES =
      List.of("GET scenario - Required");

  @Test
  void selectsProducerModulesOnly() {
    assertEquals(PRODUCER_MODULES, moduleNamesFor(Set.of(OvsRole.PRODUCER.getConfigName())));
  }

  @Test
  void selectsConsumerModulesOnly() {
    assertEquals(CONSUMER_MODULES, moduleNamesFor(Set.of(OvsRole.CONSUMER.getConfigName())));
  }

  @Test
  void allInOneSelectionPreservesEveryRoleModule() {
    assertEquals(
        List.of(
            "GET scenarios for supported filtering combinations - Alternative required path",
            "GET scenario for pagination - Optional/report-only",
            "GET scenario - Required"),
        moduleNamesFor(
            new LinkedHashSet<>(
                List.of(
                    OvsRole.PRODUCER.getConfigName(), OvsRole.CONSUMER.getConfigName()))));
  }

  @Test
  void scenarioClassificationsMatchRequirementsForEveryRoleSelection() {
    Map<String, ScenarioConformanceType> expectedTypes = Map.of(
        PRODUCER_MODULES.get(0), ScenarioConformanceType.INTERCHANGEABLE,
        PRODUCER_MODULES.get(1), ScenarioConformanceType.OPTIONAL,
        CONSUMER_MODULES.get(0), ScenarioConformanceType.REQUIRED);
    for (Set<String> roles : List.of(
        Set.of(OvsRole.PRODUCER.getConfigName()),
        Set.of(OvsRole.CONSUMER.getConfigName()),
        Set.of(OvsRole.PRODUCER.getConfigName(), OvsRole.CONSUMER.getConfigName()))) {
      buildersFor(roles).forEach((moduleName, builder) -> {
        List<ConformanceScenario> scenarios = builder.buildScenarioList(0);
        assertEquals(moduleName.equals(PRODUCER_MODULES.get(0)) ? 10 : 1,
            scenarios.size(), moduleName);
        scenarios.forEach(scenario -> assertEquals(expectedTypes.get(moduleName),
            scenario.getConformanceType(), moduleName + " / " + scenario.getTitle()));
      });
    }
  }

  private static List<String> moduleNamesFor(Set<String> roles) {
    return List.copyOf(buildersFor(roles).keySet());
  }

  private static Map<String, OvsScenarioListBuilder> buildersFor(Set<String> roles) {
    return OvsScenarioListBuilder.createModuleScenarioListBuilders(
        COMPONENT_FACTORY, roles, "Producer1", "Consumer1");
  }
}

