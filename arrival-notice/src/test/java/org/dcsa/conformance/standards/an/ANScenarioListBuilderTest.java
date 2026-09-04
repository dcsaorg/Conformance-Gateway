package org.dcsa.conformance.standards.an;

import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioConformanceType;
import org.dcsa.conformance.standards.an.party.ANRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ANScenarioListBuilderTest {

  private static final ANComponentFactory FACTORY =
    new ANComponentFactory("AN", "1.0.0", "Conformance");

  private static final List<String> PRODUCER_MODULES =
    List.of(
      "AN Producer: GET scenarios for the required query parameter filter — Required",
      "AN Producer: GET scenario for optional query parameter filters — Optional/report-only",
      "AN Producer: GET scenario for pagination — Optional/report-only",
      "AN Producer: POST scenarios for full arrival notices — Optional/report-only",
      "AN Producer: POST scenario for arrival notice notifications — Optional/report-only");

  private static final List<String> CONSUMER_MODULES =
    List.of(
      "AN Consumer: GET scenario — Required",
      "AN Consumer: POST scenarios — Optional/report-only");

  @Test
  void selectsDocumentedModulesPerRoleAndPreservesAllInOneModules() {
    assertEquals(PRODUCER_MODULES, List.copyOf(builders(Set.of(ANRole.PRODUCER.getConfigName())).keySet()));
    assertEquals(CONSUMER_MODULES, List.copyOf(builders(Set.of(ANRole.CONSUMER.getConfigName())).keySet()));

    List<String> expected = new ArrayList<>(PRODUCER_MODULES);
    expected.addAll(CONSUMER_MODULES);
    assertEquals(
      expected,
      List.copyOf(
        builders(
          new LinkedHashSet<>(
            List.of(
              ANRole.PRODUCER.getConfigName(), ANRole.CONSUMER.getConfigName())))
          .keySet()));
  }

  @Test
  void scenarioTitlesAreUniqueAndConformanceTypesMatchDocumentation() {
    List<ConformanceScenario> scenarios = new ArrayList<>();
    long moduleIndex = 0;
    for (ANScenarioListBuilder builder :
      builders(
        new LinkedHashSet<>(
          List.of(ANRole.PRODUCER.getConfigName(), ANRole.CONSUMER.getConfigName())))
        .values()) {
      scenarios.addAll(builder.buildScenarioList(moduleIndex++));
    }

    assertEquals(12, scenarios.size());
    assertEquals(12, scenarios.stream().map(ConformanceScenario::getTitle).distinct().count());
    assertEquals(
      3,
      scenarios.stream()
        .filter(scenario -> scenario.getConformanceType() == ScenarioConformanceType.INTERCHANGEABLE)
        .count());
    assertEquals(
      8,
      scenarios.stream()
        .filter(scenario -> scenario.getConformanceType() == ScenarioConformanceType.OPTIONAL)
        .count());
    assertEquals(
      1,
      scenarios.stream()
        .filter(scenario -> scenario.getConformanceType() == ScenarioConformanceType.REQUIRED)
        .count());
  }

  private static Map<String, ANScenarioListBuilder> builders(Set<String> roles) {
    return ANScenarioListBuilder.createModuleScenarioListBuilders(
      FACTORY, roles, "Producer1", "Consumer1");
  }
}

