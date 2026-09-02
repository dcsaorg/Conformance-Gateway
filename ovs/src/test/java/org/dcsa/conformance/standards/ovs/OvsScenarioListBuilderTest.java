package org.dcsa.conformance.standards.ovs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private static List<String> moduleNamesFor(Set<String> roles) {
    Map<String, OvsScenarioListBuilder> builders =
        OvsScenarioListBuilder.createModuleScenarioListBuilders(
            COMPONENT_FACTORY, roles, "Producer1", "Consumer1");
    return List.copyOf(builders.keySet());
  }
}

