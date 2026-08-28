package org.dcsa.conformance.standards.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioConformanceType;
import org.dcsa.conformance.standards.booking.action.ShipperGetBookingSkippableAction;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.junit.jupiter.api.Test;

class BookingScenarioListBuilderTest {

  private static final String OPTIONAL_SCENARIOS = "Optional (report-only) scenarios";
  private static final BookingComponentFactory COMPONENT_FACTORY =
    new BookingComponentFactory("Booking", "2.0.0", BookingScenarioListBuilder.SCENARIO_SUITE_CONFORMANCE);

  @Test
  void carrierScenarioPathsMatchConformanceDocument() {
    Map<String, BookingScenarioListBuilder> builders = buildersFor(BookingRole.CARRIER);

    assertEquals(
      List.of("SupplyCSP [Dry cargo] - UC1 - UC5 - GET"),
      titles(builders.get("Required Dry Cargo scenario")));
    assertEquals(
      List.of(
        "SupplyCSP [Dry cargo] - UC1 - UC3",
        "SupplyCSP [Dry cargo] - UC1 - UC5 - UC7"),
      titles(builders.get("Additional required Dry Cargo scenarios (execute at least one of these two)")));
    assertEquals(
      List.of(
        "SupplyCSP [any BKG] - UC1 - GET",
        "SupplyCSP [any BKG] - UC1 - UC2 - GET",
        "SupplyCSP [any BKG] - UC1 - UC4 - GET",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC6 - GET",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC7 - UC8 (Confirm) - GET",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC7 - UC8 (Decline) - GET",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC7 - GET (amended content)",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC7 - UC9 - GET",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC10 - GET",
        "SupplyCSP [any BKG] - UC1 - UC11 - GET",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC12 - GET",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC13 - GET",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC13 - UC14 (Confirm) - GET",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC13 - UC14 (Decline) - GET"),
      titles(builders.get(OPTIONAL_SCENARIOS)));
  }

  @Test
  void shipperScenarioPathsMatchConformanceDocument() {
    Map<String, BookingScenarioListBuilder> builders = buildersFor(BookingRole.SHIPPER);

    assertEquals(
      List.of(
        "SupplyCSP [Dry cargo] - UC1 - GET",
        "SupplyCSP [Dry cargo] - UC1 - UC2 - UC3",
        "SupplyCSP [Dry cargo] - UC1 - UC5 - UC6 - UC7"),
      titles(builders.get("Required Dry Cargo scenario")));
    assertEquals(
      List.of(
        "SupplyCSP [any BKG] - UC1 - UC5 - UC7 - GET (amended content)",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC7 - UC9",
        "SupplyCSP [any BKG] - UC1 - UC5 - UC13",
        "SupplyCSP [any BKG] - UC1 - UC11"),
      titles(builders.get(OPTIONAL_SCENARIOS)));
  }

  @Test
  void optionalScenariosAreReportOnlyAndNoScenarioUsesSkippableGet() {
    for (BookingRole role : BookingRole.values()) {
      Map<String, BookingScenarioListBuilder> builders = buildersFor(role);
      builders.forEach((section, builder) ->
        builder.buildScenarioList(0).forEach(scenario -> {
          assertEquals(
            section.equals(OPTIONAL_SCENARIOS)
              ? ScenarioConformanceType.OPTIONAL
              : ScenarioConformanceType.REQUIRED,
            scenario.getConformanceType());
          assertFalse(
            scenario.allActionsStream()
              .anyMatch(ShipperGetBookingSkippableAction.class::isInstance));
        }));
    }
  }

  private static Map<String, BookingScenarioListBuilder> buildersFor(BookingRole role) {
    return BookingScenarioListBuilder.createModuleScenarioListBuilders(
      COMPONENT_FACTORY,
      Set.of(role.getConfigName()),
      false,
      "Carrier",
      "Shipper");
  }

  private static List<String> titles(BookingScenarioListBuilder builder) {
    return builder.buildScenarioList(0).stream().map(ConformanceScenario::getTitle).toList();
  }
}


