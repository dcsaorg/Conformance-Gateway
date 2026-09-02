package org.dcsa.conformance.standards.tnt.v300;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.standards.tnt.v300.party.TntRole;
import org.junit.jupiter.api.Test;

class TntScenarioListBuilderTest {

  private static final TntComponentFactory COMPONENT_FACTORY =
    new TntComponentFactory("Track and Trace", "3.0.0", "Conformance");

  @Test
  void singleRoleScenarioTitlesRemainUnqualified() {
    for (TntRole role : TntRole.values()) {
      assertTrue(titlesFor(Set.of(role.getConfigName())).stream()
        .noneMatch(TntScenarioListBuilderTest::hasRolePrefix));
    }
  }

  @Test
  void allInOneScenarioTitlesAreRoleQualifiedAndUnique() {
    List<String> titles = titlesFor(Set.of(
      TntRole.PRODUCER.getConfigName(), TntRole.CONSUMER.getConfigName()));

    assertTrue(titles.stream().allMatch(TntScenarioListBuilderTest::hasRolePrefix));
    assertEquals(titles.size(), new HashSet<>(titles).size());
  }

  private static List<String> titlesFor(Set<String> roles) {
    Map<String, TntScenarioListBuilder> builders =
      TntScenarioListBuilder.createModuleScenarioListBuilders(
        COMPONENT_FACTORY, roles, "Producer1", "Consumer1");
    return builders.values().stream()
      .flatMap(builder -> builder.buildScenarioList(0).stream())
      .map(ConformanceScenario::getTitle)
      .toList();
  }

  private static boolean hasRolePrefix(String title) {
    return title.startsWith("Producer: ") || title.startsWith("Consumer: ");
  }
}

