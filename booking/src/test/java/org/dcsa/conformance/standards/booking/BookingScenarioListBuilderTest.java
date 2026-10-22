package org.dcsa.conformance.standards.booking;

import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioConformanceType;
import org.dcsa.conformance.standards.booking.action.BookingAction;
import org.dcsa.conformance.standards.booking.action.CarrierNotificationBookingAction;
import org.dcsa.conformance.standards.booking.action.ShipperGetBookingSkippableAction;
import org.dcsa.conformance.standards.booking.action.ShipperNotificationBookingAction;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        "UC1[Dry cargo] - GET",
        "UC1[Dry cargo] - UC2 - UC3",
        "UC1[Dry cargo] - UC5 - UC6 - UC7"),
      titles(builders.get("Required Dry Cargo scenario")));
    assertEquals(
      List.of(
        "UC1 - UC5 - UC7 - GET (amended content)",
        "UC1 - UC5 - UC7 - UC9",
        "UC1 - UC5 - UC13",
        "UC1 - UC11"),
      titles(builders.get(OPTIONAL_SCENARIOS)));
  }

  @Test
  void allInOneScenarioModulesPreserveCollidingLabelsForBothRoles() {
    Map<String, BookingScenarioListBuilder> builders =
      BookingScenarioListBuilder.createModuleScenarioListBuilders(
        COMPONENT_FACTORY,
        Set.of(BookingRole.CARRIER.getConfigName(), BookingRole.SHIPPER.getConfigName()),
        false,
        "Carrier",
        "Shipper");

    assertEquals(11, builders.size());
    assertTrue(builders.containsKey("Carrier: Required Dry Cargo scenario"));
    assertTrue(builders.containsKey("Shipper: Required Dry Cargo scenario"));
    assertTrue(builders.containsKey("Carrier: Required Dangerous Goods scenario"));
    assertTrue(builders.containsKey("Shipper: Required Dangerous Goods scenario"));
    assertTrue(builders.containsKey("Carrier: " + OPTIONAL_SCENARIOS));
    assertTrue(builders.containsKey("Shipper: " + OPTIONAL_SCENARIOS));
    assertScenarioClassifications(builders);
  }

  @Test
  void scenarioClassificationsMatchConformanceRequirementsAndNoScenarioUsesSkippableGet() {
    for (BookingRole role : BookingRole.values()) {
      Map<String, BookingScenarioListBuilder> builders = buildersFor(role);
      assertScenarioClassifications(builders);
      builders.forEach((section, builder) ->
        builder.buildScenarioList(0).forEach(scenario -> {
          assertFalse(
            scenario.allActionsStream()
              .anyMatch(ShipperGetBookingSkippableAction.class::isInstance));
        }));
    }
  }

  @Test
  void eachCarrierCargoQualifierHasItsOwnPairOfInterchangeableScenarios() {
    Map<String, BookingScenarioListBuilder> builders = buildersFor(BookingRole.CARRIER);
    for (String cargoQualifier : List.of("Dry Cargo", "Reefer", "Dangerous Goods")) {
      String section = "Additional required %s scenarios (execute at least one of these two)"
        .formatted(cargoQualifier);
      List<ConformanceScenario> scenarios = builders.get(section).buildScenarioList(0);
      assertEquals(2, scenarios.size(), section);
      scenarios.forEach(scenario -> assertEquals(
        ScenarioConformanceType.INTERCHANGEABLE, scenario.getConformanceType(), scenario.getTitle()));
    }
  }

  @Test
  void mandatoryNotificationsIgnoreSecondaryStatusesButOptionalNotificationsRemainStrict() {
    for (BookingRole role : BookingRole.values()) {
      buildersFor(role).forEach((section, builder) ->
        builder.buildScenarioList(0).forEach(scenario ->
          scenario.allActionsStream()
            .filter(action ->
              action instanceof CarrierNotificationBookingAction
                || action instanceof ShipperNotificationBookingAction)
            .map(BookingAction.class::cast)
            .forEach(action -> assertEquals(
              section.equals(OPTIONAL_SCENARIOS),
              action.isSecondaryStatusValidationEnabled(),
              scenario.getTitle() + " / " + action.getActionTitle()))));
    }
  }

  @Test
  void shipperScenarioTitlesAreUniqueAcrossAllSections() {
    Map<String, BookingScenarioListBuilder> builders = buildersFor(BookingRole.SHIPPER);
    List<String> allTitles = builders.values().stream()
      .flatMap(builder -> titles(builder).stream())
      .toList();
    assertEquals(
      allTitles.size(),
      new HashSet<>(allTitles).size(),
      "Shipper scenario titles must be unique");
    assertTrue(
      allTitles.stream().anyMatch(title -> title.startsWith("UC1[Dry cargo]")),
      "Expected dry cargo shipper title prefix");
  }

  private static void assertScenarioClassifications(Map<String, BookingScenarioListBuilder> builders) {
    builders.forEach((section, builder) -> {
      ScenarioConformanceType expected = section.endsWith(OPTIONAL_SCENARIOS)
        ? ScenarioConformanceType.OPTIONAL
        : section.startsWith("Additional required ")
          ? ScenarioConformanceType.INTERCHANGEABLE
          : ScenarioConformanceType.REQUIRED;
      builder.buildScenarioList(0).forEach(scenario ->
        assertEquals(expected, scenario.getConformanceType(), section + " / " + scenario.getTitle()));
    });
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


