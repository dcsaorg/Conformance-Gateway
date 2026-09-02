package org.dcsa.conformance.standards.cs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dcsa.conformance.standards.cs.party.CsRole;
import org.junit.jupiter.api.Test;

class CsScenarioListBuilderTest {

  private static final CsComponentFactory COMPONENT_FACTORY =
      new CsComponentFactory("CS", "1.0.0", "Conformance");
  private static final List<String> PRODUCER_MODULES =
      List.of(
          "Point-to-Point: Required query parameter scenario — Required",
          "Point-to-Point: Optional query parameter scenario — Optional/report-only",
          "Point-to-Point: Pagination scenario — Optional/report-only",
          "Port Schedules: Required query parameter scenario — Required",
          "Port Schedules: Pagination scenario — Optional/report-only",
          "Vessel Schedules - Service :  Required query parameter scenario",
          "Vessel Schedules - Vessel : Required query parameter scenario",
          "Vessel Schedules -  Voyage : Required query parameter scenario",
          "Vessel Schedules - Location : Required query parameter scenarios",
          "Vessel Schedules: Optional query parameter scenario — Optional/report-only",
          "Vessel Schedules: Pagination scenario — Optional/report-only");
  private static final List<String> CONSUMER_MODULES =
      List.of(
          "Point-to-Point: GET scenario - Required",
          "Port Schedules: GET scenario - Required",
          "Vessel Schedules: GET scenario - Required");

  @Test
  void selectsProducerModulesOnly() {
    assertEquals(PRODUCER_MODULES, moduleNamesFor(Set.of(CsRole.PRODUCER.getConfigName())));
  }

  @Test
  void selectsConsumerModulesOnly() {
    assertEquals(CONSUMER_MODULES, moduleNamesFor(Set.of(CsRole.CONSUMER.getConfigName())));
  }

  @Test
  void allInOneSelectionPreservesEveryRoleModule() {
    List<String> expectedModules = new java.util.ArrayList<>(PRODUCER_MODULES);
    expectedModules.addAll(CONSUMER_MODULES);

    assertEquals(
        expectedModules,
        moduleNamesFor(
            new LinkedHashSet<>(
                List.of(
                    CsRole.PRODUCER.getConfigName(), CsRole.CONSUMER.getConfigName()))));
  }

  private static List<String> moduleNamesFor(Set<String> roles) {
    Map<String, CsScenarioListBuilder> builders =
        CsScenarioListBuilder.createModuleScenarioListBuilders(
            COMPONENT_FACTORY, roles, "Producer1", "Consumer1");
    return List.copyOf(builders.keySet());
  }
}

