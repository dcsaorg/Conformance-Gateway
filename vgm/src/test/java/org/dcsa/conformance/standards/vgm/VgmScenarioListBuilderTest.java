package org.dcsa.conformance.standards.vgm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.standards.vgm.party.VgmRole;
import org.junit.jupiter.api.Test;

class VgmScenarioListBuilderTest {

  private static final VgmComponentFactory COMPONENT_FACTORY =
    new VgmComponentFactory("Verified Gross Mass", "1.0.0", "Conformance");

  @Test
  void singleRoleScenarioTitlesRemainUnqualified() {
    for (VgmRole role : VgmRole.values()) {
      assertTrue(titlesFor(Set.of(role.getConfigName())).stream()
        .noneMatch(VgmScenarioListBuilderTest::hasRolePrefix));
    }
  }

  @Test
  void allInOneScenarioTitlesAreRoleQualifiedAndUnique() {
    List<String> titles = titlesFor(Set.of(
      VgmRole.PRODUCER.getConfigName(), VgmRole.CONSUMER.getConfigName()));

    assertTrue(titles.stream().allMatch(VgmScenarioListBuilderTest::hasRolePrefix));
    assertEquals(titles.size(), new HashSet<>(titles).size());
  }

  private static List<String> titlesFor(Set<String> roles) {
    Map<String, VgmScenarioListBuilder> builders =
      VgmScenarioListBuilder.createModuleScenarioListBuilders(
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

