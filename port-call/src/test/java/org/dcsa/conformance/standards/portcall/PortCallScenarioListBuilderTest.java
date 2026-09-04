package org.dcsa.conformance.standards.portcall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.standards.portcall.party.PortCallRole;
import org.junit.jupiter.api.Test;

class PortCallScenarioListBuilderTest {

  private static final PortCallComponentFactory COMPONENT_FACTORY =
    new PortCallComponentFactory("Port Call", "2.0.0", "Conformance");

  @Test
  void singleRoleScenarioTitlesRemainUnqualified() {
    for (PortCallRole role : PortCallRole.values()) {
      assertTrue(titlesFor(Set.of(role.getConfigName())).stream()
        .noneMatch(PortCallScenarioListBuilderTest::hasRolePrefix));
    }
  }

  @Test
  void allInOneScenarioTitlesAreRoleQualifiedAndUnique() {
    List<String> titles = titlesFor(Set.of(
      PortCallRole.PRODUCER.getConfigName(), PortCallRole.CONSUMER.getConfigName()));

    assertTrue(titles.stream().allMatch(PortCallScenarioListBuilderTest::hasRolePrefix));
    assertEquals(titles.size(), new HashSet<>(titles).size());
  }

  private static List<String> titlesFor(Set<String> roles) {
    Map<String, PortCallScenarioListBuilder> builders =
      PortCallScenarioListBuilder.createModuleScenarioListBuilders(
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


