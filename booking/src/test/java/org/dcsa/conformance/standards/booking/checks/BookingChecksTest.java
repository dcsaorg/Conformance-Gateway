package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standardscommons.party.BookingDynamicScenarioParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingChecksTest {

  private static final String CBRR = "CBRR123";
  private static final String CBR = "CBR456";

  private static final Supplier<BookingDynamicScenarioParameters> DRY_CARGO_PARAMETERS =
    parametersFor(ScenarioType.DRY_CARGO);
  private static final Supplier<BookingDynamicScenarioParameters> REEFER_PARAMETERS =
    parametersFor(ScenarioType.REEFER);
  private static final Supplier<BookingDynamicScenarioParameters> DG_PARAMETERS =
    parametersFor(ScenarioType.DG);
  private static final Supplier<BookingDynamicScenarioParameters> ANY_PARAMETERS =
    parametersFor(ScenarioType.ANY);

  // ---------------------------------------------------------------------------------------------
  // Fixtures and helpers
  // ---------------------------------------------------------------------------------------------

  private static Supplier<BookingDynamicScenarioParameters> parametersFor(ScenarioType type) {
    return () -> new BookingDynamicScenarioParameters(type.name(), CBRR, CBR);
  }

  private static ObjectNode body(String json) {
    try {
      return (ObjectNode) OBJECT_MAPPER.readTree(json);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid test JSON: " + json, e);
    }
  }

  private static ObjectNode emptyBody() {
    return OBJECT_MAPPER.createObjectNode();
  }

  /**
   * Resolves a carrier-payload validation by a unique fragment of its description.
   */
  private static JsonContentCheck carrierCheck(String descriptionFragment) {
    return findCheck(
      BookingChecks.fullPayloadChecks(
        DRY_CARGO_PARAMETERS, CarrierStatusScenario.from(BookingState.CONFIRMED, null, null)),
      descriptionFragment);
  }

  /**
   * Resolves a scenario-specific validation for the given scenario parameters.
   */
  private static JsonContentCheck scenarioCheck(
    Supplier<BookingDynamicScenarioParameters> parameters, String descriptionFragment) {
    return findCheck(BookingChecks.generateScenarioRelatedChecks(parameters), descriptionFragment);
  }

  private static JsonContentCheck findCheck(
    List<JsonContentCheck> checks, String descriptionFragment) {
    List<JsonContentCheck> matches =
      checks.stream().filter(check -> check.description().contains(descriptionFragment)).toList();
    if (matches.size() != 1) {
      throw new IllegalArgumentException(
        "Expected exactly one check matching '%s' but found %d"
          .formatted(descriptionFragment, matches.size()));
    }
    return matches.getFirst();
  }

  private static void assertConformant(JsonContentCheck check, JsonNode payload) {
    ConformanceCheckResult result = check.validate(payload);
    assertTrue(result.isConformant(), () -> "Unexpected errors: " + result.getErrorMessages());
  }

  private static void assertNotConformant(JsonContentCheck check, JsonNode payload) {
    assertFalse(check.validate(payload).isConformant(), check::description);
  }

  private static void assertIrrelevant(JsonContentCheck check, JsonNode payload) {
    ConformanceCheckResult result = check.validate(payload);
    assertFalse(result.isRelevant(), check::description);
    assertTrue(result.isConformant(), () -> "Unexpected errors: " + result.getErrorMessages());
  }

  private static void assertErrorReported(
    JsonContentCheck check, JsonNode payload, String expectedMessage) {
    Set<String> errors = check.validate(payload).getErrorMessages();
    assertTrue(errors.contains(expectedMessage), () -> "Actual errors: " + errors);
  }

  // ---------------------------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Public check factories")
  class PublicApi {

    @Test
    void givenScenarioParameters_whenBuildingRequestContentChecks_thenCheckIsCreated() {
      assertNotNull(
        BookingChecks.requestContentChecks(UUID.randomUUID(), "2.0.4", DRY_CARGO_PARAMETERS));
    }

    @Test
    void givenScenarioParameters_whenBuildingUpdateRequestContentChecks_thenCheckIsCreated() {
      assertNotNull(
        BookingChecks.updateRequestContentChecks(
          UUID.randomUUID(), "2.0.4", DRY_CARGO_PARAMETERS));
    }

    @Test
    void givenScenarioParameters_whenBuildingResponseContentChecks_thenCheckIsCreated() {
      assertNotNull(
        BookingChecks.responseContentChecks(
          UUID.randomUUID(),
          "2.0.4",
          DRY_CARGO_PARAMETERS,
          CarrierStatusScenario.from(BookingState.CONFIRMED, null, null)));
    }

    @Test
    void givenCarrierStatusScenario_whenBuildingFullPayloadChecks_thenStatusCheckIsIncluded() {
      List<JsonContentCheck> checks =
        BookingChecks.fullPayloadChecks(
          DRY_CARGO_PARAMETERS, CarrierStatusScenario.from(BookingState.CONFIRMED, null, null));

      assertTrue(
        checks.stream()
          .anyMatch(check -> check.description().contains("must match the active scenario")));
    }

    @Test
    void givenScenarioParameters_whenBuildingNotificationChecks_thenEnvelopeChecksAreExcluded() {
      List<JsonContentCheck> checks =
        BookingChecks.nestedNotificationPayloadChecks(DRY_CARGO_PARAMETERS);

      assertFalse(
        checks.stream()
          .anyMatch(check -> check.description().contains("must match the active scenario")));
    }

    @Test
    void givenFeedbackSeverityDataset_whenReadingDescription_thenAllowedValuesAreListed() {
      assertEquals(
        "The 'feedbacks.severity' attribute must demonstrate the correct use of a feedback severity code: INFO, WARN, ERROR",
        BookingChecks.VALID_FEEDBACK_SEVERITY.description());
    }

    @Test
    void givenFeedbackCodeDataset_whenReadingDescription_thenAllowedValuesAreListed() {
      assertEquals(
        "The 'feedbacks.code' attribute must demonstrate the correct use of a feedback code: INFORMATIONAL_MESSAGE, PROPERTY_WILL_BE_IGNORED, PROPERTY_VALUE_MUST_CHANGE, PROPERTY_VALUE_HAS_BEEN_CHANGED, PROPERTY_VALUE_MAY_CHANGE, PROPERTY_HAS_BEEN_DELETED",
        BookingChecks.VALID_FEEDBACK_CODE.description());
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Scenario (cargo type) validations
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Scenario validations")
  class ScenarioValidations {

    private static final String REEFER_FRAGMENT = "Reefer scenario requires at least one";
    private static final String DRY_CONTAINER_FRAGMENT = "Dry cargo container validation";
    private static final String DRY_AND_REEFER_DG_FRAGMENT = "Dry cargo and Reefer scenarios require";
    private static final String DG_PRESENT_FRAGMENT = "DG scenario requires";
    private static final String DG_OUTER_PACKAGING_FRAGMENT = "Mandatory for DG";

    @Test
    void givenReeferScenario_whenOneEquipmentHasActiveReeferSettings_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{},{"activeReeferSettings":{"temperatureSetpoint":2}}]}
            """);

      assertConformant(scenarioCheck(REEFER_PARAMETERS, REEFER_FRAGMENT), payload);
    }

    @Test
    void givenReeferScenario_whenNoEquipmentHasActiveReeferSettings_thenErrorIsReported() {
      JsonNode payload = body("""
        {"requestedEquipments":[{},{}]}
        """);

      assertErrorReported(
        scenarioCheck(REEFER_PARAMETERS, REEFER_FRAGMENT),
        payload,
        "The scenario requires at least one 'requestedEquipments[*].activeReeferSettings' to be present");
    }

    @Test
    void givenDryCargoScenario_whenReeferCheckIsEvaluated_thenCheckIsIrrelevant() {
      assertIrrelevant(scenarioCheck(DRY_CARGO_PARAMETERS, REEFER_FRAGMENT), emptyBody());
    }

    @Test
    void givenDryCargoScenario_whenActiveReeferSettingsPresent_thenErrorIsReported() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"activeReeferSettings":{}}]}
        """);

      assertErrorReported(
        scenarioCheck(DRY_CARGO_PARAMETERS, DRY_CONTAINER_FRAGMENT),
        payload,
        "The scenario requires 'requestedEquipments[0].activeReeferSettings' to be absent");
    }

    @Test
    void givenDryCargoScenario_whenNonOperatingReeferIsFalse_thenCheckPasses() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"isNonOperatingReefer":false}]}
        """);

      assertConformant(scenarioCheck(DRY_CARGO_PARAMETERS, DRY_CONTAINER_FRAGMENT), payload);
    }

    @Test
    void givenDryCargoScenario_whenNonOperatingReeferIsTrue_thenErrorIsReported() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"isNonOperatingReefer":true}]}
        """);

      assertErrorReported(
        scenarioCheck(DRY_CARGO_PARAMETERS, DRY_CONTAINER_FRAGMENT),
        payload,
        "The scenario requires 'requestedEquipments[0].isNonOperatingReefer' to be absent or set to false");
    }

    @Test
    void givenDryCargoScenario_whenNonOperatingReeferIsNonBoolean_thenErrorIsReported() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"isNonOperatingReefer":"false"}]}
        """);

      assertErrorReported(
        scenarioCheck(DRY_CARGO_PARAMETERS, DRY_CONTAINER_FRAGMENT),
        payload,
        "The scenario requires 'requestedEquipments[0].isNonOperatingReefer' to be absent or set to false");
    }

    @Test
    void givenDryCargoScenario_whenNoReeferAttributesPresent_thenCheckPasses() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"ISOEquipmentCode":"22G1"}]}
        """);

      assertConformant(scenarioCheck(DRY_CARGO_PARAMETERS, DRY_CONTAINER_FRAGMENT), payload);
    }

    @Test
    void givenDryCargoScenario_whenDangerousGoodsPresent_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[{}]}}]}]}
            """);

      assertErrorReported(
        scenarioCheck(DRY_CARGO_PARAMETERS, DRY_AND_REEFER_DG_FRAGMENT),
        payload,
        "The scenario requires 'requestedEquipments[0].commodities[0].outerPackaging.dangerousGoods' to NOT contain any dangerous goods");
    }

    @Test
    void givenDryCargoScenario_whenDangerousGoodsAbsent_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"requestedEquipments":[{"commodities":[{"outerPackaging":{}}]}]}
          """);

      assertConformant(scenarioCheck(DRY_CARGO_PARAMETERS, DRY_AND_REEFER_DG_FRAGMENT), payload);
    }

    @Test
    void givenDryCargoScenario_whenDangerousGoodsArrayIsEmpty_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[]}}]}]}
            """);

      assertConformant(scenarioCheck(DRY_CARGO_PARAMETERS, DRY_AND_REEFER_DG_FRAGMENT), payload);
    }

    @Test
    void givenDgScenario_whenDangerousGoodsArrayIsEmpty_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[]}}]}]}
            """);

      assertErrorReported(
        scenarioCheck(DG_PARAMETERS, DG_PRESENT_FRAGMENT),
        payload,
        "The scenario requires 'requestedEquipments[0].commodities[0].outerPackaging.dangerousGoods' to contain dangerous goods");
    }

    @Test
    void givenDgScenario_whenDangerousGoodsAbsent_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"requestedEquipments":[{"commodities":[{"outerPackaging":{}}]}]}
          """);

      assertErrorReported(
        scenarioCheck(DG_PARAMETERS, DG_PRESENT_FRAGMENT),
        payload,
        "The scenario requires 'requestedEquipments[0].commodities[0].outerPackaging.dangerousGoods' to contain dangerous goods");
    }

    @Test
    void givenDgScenario_whenDangerousGoodsPresent_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[{}]}}]}]}
            """);

      assertConformant(scenarioCheck(DG_PARAMETERS, DG_PRESENT_FRAGMENT), payload);
    }

    @Test
    void givenDgScenario_whenOuterPackagingAbsent_thenErrorIsReported() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"commodities":[{}]}]}
        """);

      assertErrorReported(
        scenarioCheck(DG_PARAMETERS, DG_OUTER_PACKAGING_FRAGMENT),
        payload,
        "'requestedEquipments[0].commodities[0].outerPackaging' is mandatory for dangerous-goods cargo");
    }

    @Test
    void givenDgScenario_whenOuterPackagingPresent_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"numberOfPackages":2}}]}]}
            """);

      assertConformant(scenarioCheck(DG_PARAMETERS, DG_OUTER_PACKAGING_FRAGMENT), payload);
    }

    @Test
    void givenAnyScenario_whenOuterPackagingCheckIsEvaluated_thenCheckIsIrrelevant() {
      assertIrrelevant(scenarioCheck(ANY_PARAMETERS, DG_OUTER_PACKAGING_FRAGMENT), emptyBody());
    }

    @Test
    void givenAnyScenario_whenCargoTypeChecksAreEvaluated_thenNoErrorIsReported() {
      BookingChecks.generateScenarioRelatedChecks(ANY_PARAMETERS)
        .forEach(check -> assertConformant(check, emptyBody()));
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Requested equipment validations
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Active reefer settings")
  class ActiveReeferSettings {

    private static final String TEMPERATURE_FRAGMENT = "activeReeferSettings.temperatureUnit";
    private static final String AIR_EXCHANGE_FRAGMENT = "activeReeferSettings.airExchangeUnit";

    @Test
    void givenEquipment_whenIsoCodeIsMissing_thenReeferConsistencyCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"requestedEquipments":[{}]}
        """);

      assertIrrelevant(BookingChecks.NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER, payload);
    }

    @Test
    void givenEquipment_whenIsoCodeIsShorterThanThreeCharacters_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"ISOEquipmentCode":"22"}]}
        """);

      assertIrrelevant(BookingChecks.NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER, payload);
    }

    @Test
    void givenDryEquipment_whenIsoCodeIsNotReefer_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"ISOEquipmentCode":"22G1"}]}
        """);

      assertIrrelevant(BookingChecks.NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER, payload);
    }

    @Test
    void givenReeferEquipment_whenNonOperatingReeferIsTrue_thenCheckIsIrrelevant() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"ISOEquipmentCode":"22R1","isNonOperatingReefer":true}]}
            """);

      assertIrrelevant(BookingChecks.NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER, payload);
    }

    @Test
    void givenReeferEquipment_whenNonOperatingReeferIsMissing_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"ISOEquipmentCode":"22R1"}]}
        """);

      assertIrrelevant(BookingChecks.NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER, payload);
    }

    @Test
    void givenOperatingReefer_whenActiveReeferSettingsAreMissing_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"ISOEquipmentCode":"22R1","isNonOperatingReefer":false}]}
            """);

      assertErrorReported(
        BookingChecks.NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER,
        payload,
        "The attribute 'requestedEquipments[0].activeReeferSettings' should have been present but was absent");
    }

    @Test
    void givenOperatingReefer_whenActiveReeferSettingsAreEmpty_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"ISOEquipmentCode":"22R1","isNonOperatingReefer":false,"activeReeferSettings":{}}]}
            """);

      assertErrorReported(
        BookingChecks.NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER,
        payload,
        "The attribute 'requestedEquipments[0].activeReeferSettings' should have been present but was absent");
    }

    @Test
    void givenOperatingReefer_whenActiveReeferSettingsArePopulated_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"ISOEquipmentCode":"22H1","isNonOperatingReefer":false,"activeReeferSettings":{"temperatureSetpoint":2}}]}
            """);

      assertConformant(BookingChecks.NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER, payload);
    }

    @Test
    void givenNoRequestedEquipments_whenReeferConsistencyIsChecked_thenCheckPasses() {
      assertConformant(BookingChecks.NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER, emptyBody());
    }

    @Test
    void givenTemperatureSetpoint_whenTemperatureUnitIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"activeReeferSettings":{"temperatureSetpoint":2}}]}
            """);

      assertErrorReported(
        carrierCheck(TEMPERATURE_FRAGMENT),
        payload,
        "'requestedEquipments[0].activeReeferSettings.temperatureUnit' must be provided if and only if 'temperatureSetpoint' is provided");
    }

    @Test
    void givenTemperatureUnit_whenTemperatureSetpointIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"requestedEquipments":[{"activeReeferSettings":{"temperatureUnit":"CEL"}}]}
          """);

      assertErrorReported(
        carrierCheck(TEMPERATURE_FRAGMENT),
        payload,
        "'requestedEquipments[0].activeReeferSettings.temperatureUnit' must be provided if and only if 'temperatureSetpoint' is provided");
    }

    @Test
    void givenTemperatureSetpointAndUnit_whenBothProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"activeReeferSettings":{"temperatureSetpoint":2,"temperatureUnit":"CEL"}}]}
            """);

      assertConformant(carrierCheck(TEMPERATURE_FRAGMENT), payload);
    }

    @Test
    void givenAirExchange_whenAirExchangeUnitIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"requestedEquipments":[{"activeReeferSettings":{"airExchange":10}}]}
          """);

      assertErrorReported(
        carrierCheck(AIR_EXCHANGE_FRAGMENT),
        payload,
        "'requestedEquipments[0].activeReeferSettings.airExchangeUnit' must be provided if and only if 'airExchange' is provided");
    }

    @Test
    void givenAirExchangeAndUnit_whenBothProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"activeReeferSettings":{"airExchange":10,"airExchangeUnit":"MQH"}}]}
            """);

      assertConformant(carrierCheck(AIR_EXCHANGE_FRAGMENT), payload);
    }
  }

  @Nested
  @DisplayName("Cargo gross weight")
  class CargoGrossWeight {

    @Test
    void givenEquipmentWeight_whenCommodityLevelIsChecked_thenCheckIsIrrelevant() {
      JsonNode payload =
        body("""
          {"requestedEquipments":[{"cargoGrossWeight":{"value":1,"unit":"KGM"}}]}
          """);

      assertIrrelevant(BookingChecks.CHECK_CARGO_GROSS_WEIGHT_AT_COMMODITY_LEVEL, payload);
    }

    @Test
    void givenNoCommodities_whenCommodityLevelIsChecked_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"requestedEquipments":[{}]}
        """);

      assertIrrelevant(BookingChecks.CHECK_CARGO_GROSS_WEIGHT_AT_COMMODITY_LEVEL, payload);
    }

    @Test
    void givenCommodityWeights_whenEquipmentWeightIsMissing_thenCommodityCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"cargoGrossWeight":{"value":1,"unit":"KGM"}}]}]}
            """);

      assertConformant(BookingChecks.CHECK_CARGO_GROSS_WEIGHT_AT_COMMODITY_LEVEL, payload);
    }

    @Test
    void givenCommodityWithoutWeight_whenEquipmentWeightIsMissing_thenErrorIsReported() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"commodities":[{}]}]}
        """);

      assertErrorReported(
        BookingChecks.CHECK_CARGO_GROSS_WEIGHT_AT_COMMODITY_LEVEL,
        payload,
        "The 'requestedEquipments[0]' must have 'cargoGrossWeight' at 'commodities' position 0");
    }

    @Test
    void givenCommodityWithNullWeight_whenEquipmentWeightIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"requestedEquipments":[{"commodities":[{"cargoGrossWeight":null}]}]}
          """);

      assertErrorReported(
        BookingChecks.CHECK_CARGO_GROSS_WEIGHT_AT_COMMODITY_LEVEL,
        payload,
        "The 'requestedEquipments[0]' must have 'cargoGrossWeight' at 'commodities' position 0");
    }

    @Test
    void givenEquipmentWeight_whenEquipmentLevelIsChecked_thenCheckIsIrrelevant() {
      JsonNode payload =
        body("""
          {"requestedEquipments":[{"cargoGrossWeight":{"value":1,"unit":"KGM"}}]}
          """);

      assertIrrelevant(BookingChecks.CHECK_CARGO_GROSS_WEIGHT_AT_EQUIPMENT_LEVEL, payload);
    }

    @Test
    void givenNoCommodities_whenEquipmentLevelIsChecked_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"requestedEquipments":[{}]}
        """);

      assertIrrelevant(BookingChecks.CHECK_CARGO_GROSS_WEIGHT_AT_EQUIPMENT_LEVEL, payload);
    }

    @Test
    void givenEveryCommodityHasWeight_whenEquipmentWeightIsMissing_thenEquipmentCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"cargoGrossWeight":{"value":1,"unit":"KGM"}}]}]}
            """);

      assertConformant(BookingChecks.CHECK_CARGO_GROSS_WEIGHT_AT_EQUIPMENT_LEVEL, payload);
    }

    @Test
    void givenOneCommodityWithoutWeight_whenEquipmentWeightIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"cargoGrossWeight":{"value":1,"unit":"KGM"}},{}]}]}
            """);

      assertErrorReported(
        BookingChecks.CHECK_CARGO_GROSS_WEIGHT_AT_EQUIPMENT_LEVEL,
        payload,
        "The 'requestedEquipments[0].cargoGrossWeight' must be provided when it is not provided on every Commodity");
    }
  }

  @Nested
  @DisplayName("Commodity sub references")
  class CommoditySubReferences {

    private static final String UNIQUE_WHEN_CONFIRMED_FRAGMENT =
      "must be unique across the entire booking when 'bookingStatus'";
    private static final String PRESENT_WHEN_CONFIRMED_FRAGMENT =
      "The 'commoditySubReference' must be present when 'bookingStatus'";

    @Test
    void givenNoRequestedEquipments_whenUniquenessIsChecked_thenCheckPasses() {
      assertConformant(BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE, emptyBody());
    }

    @Test
    void givenBlankSubReferences_whenUniquenessIsChecked_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"commoditySubReference":"  "},{"commoditySubReference":"  "}]}]}
            """);

      assertConformant(BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE, payload);
    }

    @Test
    void givenUniqueSubReferences_whenUniquenessIsChecked_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"commoditySubReference":"A"}]},{"commodities":[{"commoditySubReference":"B"}]}]}
            """);

      assertConformant(BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE, payload);
    }

    @Test
    void givenDuplicateSubReferences_whenUniquenessIsChecked_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"commoditySubReference":"A"},{"commoditySubReference":"A"}]}]}
            """);

      assertErrorReported(
        BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE,
        payload,
        "commoditySubReference 'A' is not unique across the booking. Found 2 occurrences.");
    }

    @Test
    void givenReceivedBooking_whenUniquenessByStateIsChecked_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"bookingStatus":"RECEIVED"}
        """);

      assertIrrelevant(carrierCheck(UNIQUE_WHEN_CONFIRMED_FRAGMENT), payload);
    }

    @Test
    void givenConfirmedBooking_whenSubReferencesAreDuplicated_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"bookingStatus":"CONFIRMED","requestedEquipments":[{"commodities":[{"commoditySubReference":"A"},{"commoditySubReference":"A"}]}]}
            """);

      assertErrorReported(
        carrierCheck(UNIQUE_WHEN_CONFIRMED_FRAGMENT),
        payload,
        "commoditySubReference 'A' is not unique across the booking. Found 2 occurrences.");
    }

    @Test
    void givenReceivedBooking_whenPresenceByStateIsChecked_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"bookingStatus":"RECEIVED"}
        """);

      assertIrrelevant(carrierCheck(PRESENT_WHEN_CONFIRMED_FRAGMENT), payload);
    }

    @Test
    void givenPendingAmendmentBooking_whenSubReferenceIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"bookingStatus":"PENDING_AMENDMENT","requestedEquipments":[{"commodities":[{}]}]}
            """);

      assertErrorReported(
        carrierCheck(PRESENT_WHEN_CONFIRMED_FRAGMENT),
        payload,
        "'requestedEquipments[0].commodities[0].commoditySubReference' must be present when 'bookingStatus' is PENDING_AMENDMENT");
    }

    @Test
    void givenConfirmedBooking_whenSubReferenceIsPresent_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"bookingStatus":"CONFIRMED","requestedEquipments":[{"commodities":[{"commoditySubReference":"A"}]}]}
            """);

      assertConformant(carrierCheck(PRESENT_WHEN_CONFIRMED_FRAGMENT), payload);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Routing reference and voyage validations
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Universal service reference")
  class UniversalServiceReference {

    private static final String FRAGMENT = "if either 'universalExportVoyageReference'";

    @Test
    void givenRoutingReference_whenExportVoyageReferenceIsPresent_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"routingReference":"RR","universalExportVoyageReference":"2103N"}
            """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'universalExportVoyageReference' must not be provided when 'routingReference' is provided.");
    }

    @Test
    void givenRoutingReference_whenImportVoyageReferenceIsPresent_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"routingReference":"RR","universalImportVoyageReference":"2103S"}
            """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'universalImportVoyageReference' must not be provided when 'routingReference' is provided.");
    }

    @Test
    void givenRoutingReference_whenServiceReferenceIsPresent_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"routingReference":"RR","universalServiceReference":"SR12345A"}
          """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'universalServiceReference' must not be provided when 'routingReference' is provided.");
    }

    @Test
    void givenRoutingReferenceOnly_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"routingReference":"RR"}
        """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenNoVoyageReferences_whenValidated_thenCheckIsIrrelevant() {
      assertIrrelevant(carrierCheck(FRAGMENT), emptyBody());
    }

    @Test
    void givenExportVoyageReference_whenServiceReferenceIsAbsent_thenErrorIsReported() {
      JsonNode payload = body("""
        {"universalExportVoyageReference":"2103N"}
        """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "The universalServiceReference must be present as either universalExportVoyageReference or universalImportVoyageReference are present");
    }

    @Test
    void givenExportVoyageReference_whenServiceReferenceIsPresent_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"universalExportVoyageReference":"2103N","universalServiceReference":"SR12345A"}
            """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenImportVoyageReferenceOnly_whenServiceReferenceIsAbsent_thenErrorIsReported() {
      JsonNode payload = body("""
        {"universalImportVoyageReference":"2103S"}
        """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "The universalServiceReference must be present as either universalExportVoyageReference or universalImportVoyageReference are present");
    }
  }

  @Nested
  @DisplayName("Carrier export voyage number conditions")
  class CarrierExportVoyageNumberConditions {

    private static final String FRAGMENT =
      "The 'carrierExportVoyageNumber' attribute must be provided when";

    @Test
    void givenRoutingReference_whenVoyageNumberIsPresent_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"routingReference":"RR","carrierExportVoyageNumber":"2103N"}
          """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'carrierExportVoyageNumber' must not be provided when 'routingReference' is provided.");
    }

    @Test
    void givenRoutingReference_whenVoyageNumberIsAbsent_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"routingReference":"RR"}
        """);

      assertIrrelevant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenNoRoutingReference_whenNoVoyageOrDatesProvided_thenErrorIsReported() {
      assertErrorReported(
        carrierCheck(FRAGMENT),
        emptyBody(),
        "'carrierExportVoyageNumber' must be provided when 'expectedDepartureDate' or the expected arrival dates are not provided");
    }

    @Test
    void givenNoRoutingReference_whenExpectedDepartureDateProvided_thenCheckPasses() {
      JsonNode payload = body("""
        {"expectedDepartureDate":"2026-08-01"}
        """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenNoRoutingReference_whenExpectedArrivalDatesProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"expectedArrivalAtPlaceOfDeliveryStartDate":"2026-08-01","expectedArrivalAtPlaceOfDeliveryEndDate":"2026-08-05"}
            """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenNoRoutingReference_whenOnlyArrivalStartDateProvided_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"expectedArrivalAtPlaceOfDeliveryStartDate":"2026-08-01"}
          """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'carrierExportVoyageNumber' must be provided when 'expectedDepartureDate' or the expected arrival dates are not provided");
    }

    @Test
    void givenNoRoutingReference_whenVoyageNumberProvided_thenCheckPasses() {
      JsonNode payload = body("""
        {"carrierExportVoyageNumber":"2103N"}
        """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }
  }

  @Nested
  @DisplayName("Carrier service code conditions")
  class CarrierServiceCodeConditions {

    private static final String FRAGMENT = "The 'carrierServiceCode' attribute must be provided when";

    @Test
    void givenRoutingReference_whenServiceCodeIsPresent_thenErrorIsReported() {
      JsonNode payload = body("""
        {"routingReference":"RR","carrierServiceCode":"BW1"}
        """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'carrierServiceCode' must not be provided when 'routingReference' is provided.");
    }

    @Test
    void givenRoutingReference_whenServiceCodeIsAbsent_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"routingReference":"RR"}
        """);

      assertIrrelevant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenVoyageNumber_whenNoServiceIdentificationProvided_thenErrorIsReported() {
      JsonNode payload = body("""
        {"carrierExportVoyageNumber":"2103N"}
        """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'carrierServiceCode' must be provided when 'carrierExportVoyageNumber' is provided and vessel details and 'carrierServiceName' are blank");
    }

    @Test
    void givenVoyageNumber_whenVesselNameProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","vessel":{"name":"Ship"}}
          """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenVoyageNumber_whenServiceNameProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","carrierServiceName":"Service"}
          """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenVoyageNumber_whenServiceCodeProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","carrierServiceCode":"BW1"}
          """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenNoVoyageNumber_whenServiceCodeIsMissing_thenCheckPasses() {
      assertConformant(carrierCheck(FRAGMENT), emptyBody());
    }
  }

  @Nested
  @DisplayName("Carrier service name conditions")
  class CarrierServiceNameConditions {

    private static final String FRAGMENT = "The 'carrierServiceName' attribute must be provided when";

    @Test
    void givenRoutingReference_whenServiceNameIsPresent_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"routingReference":"RR","carrierServiceName":"Service"}
          """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'carrierServiceName' must not be provided when 'routingReference' is provided.");
    }

    @Test
    void givenRoutingReference_whenServiceNameIsAbsent_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"routingReference":"RR"}
        """);

      assertIrrelevant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenVoyageNumber_whenNoServiceIdentificationProvided_thenErrorIsReported() {
      JsonNode payload = body("""
        {"carrierExportVoyageNumber":"2103N"}
        """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'carrierServiceName' must be provided when 'carrierExportVoyageNumber' is provided and vessel details and 'carrierServiceCode' are blank");
    }

    @Test
    void givenVoyageNumber_whenVesselNameProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","vessel":{"name":"Ship"}}
          """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenVoyageNumber_whenServiceCodeProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","carrierServiceCode":"BW1"}
          """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenVoyageNumber_whenServiceNameProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","carrierServiceName":"Service"}
          """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenNoVoyageNumber_whenServiceNameIsMissing_thenCheckPasses() {
      assertConformant(carrierCheck(FRAGMENT), emptyBody());
    }
  }

  @Nested
  @DisplayName("Vessel conditions")
  class VesselConditions {

    private static final String FRAGMENT = "The 'vessel' object must be provided when";

    @Test
    void givenRoutingReference_whenVesselIsPresent_thenErrorIsReported() {
      JsonNode payload = body("""
        {"routingReference":"RR","vessel":{"name":"Ship"}}
        """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'vessel' must not be provided when 'routingReference' is provided.");
    }

    @Test
    void givenRoutingReference_whenVesselIsAbsent_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"routingReference":"RR"}
        """);

      assertIrrelevant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenVoyageNumber_whenNoServiceIdentificationProvided_thenErrorIsReported() {
      JsonNode payload = body("""
        {"carrierExportVoyageNumber":"2103N"}
        """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'vessel' must be provided when 'carrierExportVoyageNumber' is provided and 'carrierServiceCode' and 'carrierServiceName' are blank");
    }

    @Test
    void givenVoyageNumber_whenServiceCodeProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","carrierServiceCode":"BW1"}
          """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenVoyageNumber_whenServiceNameProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","carrierServiceName":"Service"}
          """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenVoyageNumber_whenVesselNameProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","vessel":{"name":"Ship"}}
          """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenNoVoyageNumber_whenVesselIsMissing_thenCheckPasses() {
      assertConformant(carrierCheck(FRAGMENT), emptyBody());
    }
  }

  @Nested
  @DisplayName("Expected departure and arrival date conditions")
  class ExpectedDateConditions {

    private static final String DEPARTURE_FRAGMENT =
      "The 'expectedDepartureDate' attribute must be provided when vessel/voyage/service details";
    private static final String DEPARTURE_FROM_RECEIPT_FRAGMENT =
      "The 'expectedDepartureFromPlaceOfReceiptDate' attribute must be provided when";
    private static final String ARRIVAL_START_FRAGMENT =
      "The 'expectedArrivalAtPlaceOfDeliveryStartDate' attribute must be provided when";
    private static final String ARRIVAL_END_FRAGMENT =
      "The 'expectedArrivalAtPlaceOfDeliveryEndDate' attribute must be provided when";

    @Test
    void givenRoutingReference_whenDepartureDateIsPresent_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"routingReference":"RR","expectedDepartureDate":"2026-08-01"}
          """);

      assertErrorReported(
        carrierCheck(DEPARTURE_FRAGMENT),
        payload,
        "'expectedDepartureDate' must not be provided when 'routingReference' is provided.");
    }

    @Test
    void givenRoutingReference_whenDepartureDateIsAbsent_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"routingReference":"RR"}
        """);

      assertIrrelevant(carrierCheck(DEPARTURE_FRAGMENT), payload);
    }

    @Test
    void givenNoIdentification_whenDepartureDateIsMissing_thenErrorIsReported() {
      assertErrorReported(
        carrierCheck(DEPARTURE_FRAGMENT),
        emptyBody(),
        "'expectedDepartureDate' must be provided when vessel/voyage/service details, the expected arrival dates or 'expectedDepartureFromPlaceOfReceiptDate' are not provided");
    }

    @Test
    void givenVoyageWithVesselName_whenDepartureDateIsMissing_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","vessel":{"name":"Ship"}}
          """);

      assertConformant(carrierCheck(DEPARTURE_FRAGMENT), payload);
    }

    @Test
    void givenVoyageWithServiceCode_whenDepartureDateIsMissing_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","carrierServiceCode":"BW1"}
          """);

      assertConformant(carrierCheck(DEPARTURE_FRAGMENT), payload);
    }

    @Test
    void givenVoyageWithServiceName_whenDepartureDateIsMissing_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","carrierServiceName":"Service"}
          """);

      assertConformant(carrierCheck(DEPARTURE_FRAGMENT), payload);
    }

    @Test
    void givenExpectedArrivalDates_whenDepartureDateIsMissing_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"expectedArrivalAtPlaceOfDeliveryStartDate":"2026-08-01","expectedArrivalAtPlaceOfDeliveryEndDate":"2026-08-05"}
            """);

      assertConformant(carrierCheck(DEPARTURE_FRAGMENT), payload);
    }

    @Test
    void givenDepartureFromReceiptDate_whenDepartureDateIsMissing_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"expectedDepartureFromPlaceOfReceiptDate":"2026-08-01"}
          """);

      assertConformant(carrierCheck(DEPARTURE_FRAGMENT), payload);
    }

    @Test
    void givenDepartureDate_whenDepartureDateIsPresent_thenCheckPasses() {
      JsonNode payload = body("""
        {"expectedDepartureDate":"2026-08-01"}
        """);

      assertConformant(carrierCheck(DEPARTURE_FRAGMENT), payload);
    }

    @Test
    void givenVoyageNumberWithoutServiceIdentification_whenDepartureDateIsMissing_thenErrorIsReported() {
      JsonNode payload = body("""
        {"carrierExportVoyageNumber":"2103N"}
        """);

      assertErrorReported(
        carrierCheck(DEPARTURE_FRAGMENT),
        payload,
        "'expectedDepartureDate' must be provided when vessel/voyage/service details, the expected arrival dates or 'expectedDepartureFromPlaceOfReceiptDate' are not provided");
    }

    @Test
    void givenRoutingReference_whenDepartureFromReceiptIsChecked_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"routingReference":"RR"}
        """);

      assertIrrelevant(carrierCheck(DEPARTURE_FROM_RECEIPT_FRAGMENT), payload);
    }

    @Test
    void givenNoIdentification_whenDepartureFromReceiptIsMissing_thenErrorIsReported() {
      assertErrorReported(
        carrierCheck(DEPARTURE_FROM_RECEIPT_FRAGMENT),
        emptyBody(),
        "'expectedDepartureFromPlaceOfReceiptDate' must be provided when vessel/voyage/service details, the expected arrival dates or 'expectedDepartureDate' are not provided");
    }

    @Test
    void givenDepartureDate_whenDepartureFromReceiptIsMissing_thenCheckPasses() {
      JsonNode payload = body("""
        {"expectedDepartureDate":"2026-08-01"}
        """);

      assertConformant(carrierCheck(DEPARTURE_FROM_RECEIPT_FRAGMENT), payload);
    }

    @Test
    void givenDepartureFromReceiptDate_whenProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"expectedDepartureFromPlaceOfReceiptDate":"2026-08-01"}
          """);

      assertConformant(carrierCheck(DEPARTURE_FROM_RECEIPT_FRAGMENT), payload);
    }

    @Test
    void givenVoyageIdentification_whenDepartureFromReceiptIsMissing_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","vessel":{"name":"Ship"}}
          """);

      assertConformant(carrierCheck(DEPARTURE_FROM_RECEIPT_FRAGMENT), payload);
    }

    @Test
    void givenExpectedArrivalDates_whenDepartureFromReceiptIsMissing_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"expectedArrivalAtPlaceOfDeliveryStartDate":"2026-08-01","expectedArrivalAtPlaceOfDeliveryEndDate":"2026-08-05"}
            """);

      assertConformant(carrierCheck(DEPARTURE_FROM_RECEIPT_FRAGMENT), payload);
    }

    @Test
    void givenVoyageWithServiceNameOnly_whenDepartureFromReceiptIsMissing_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","carrierServiceName":"Service"}
          """);

      assertConformant(carrierCheck(DEPARTURE_FROM_RECEIPT_FRAGMENT), payload);
    }

    @Test
    void givenRoutingReference_whenArrivalStartDateIsPresent_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"routingReference":"RR","expectedArrivalAtPlaceOfDeliveryStartDate":"2026-08-01"}
            """);

      assertErrorReported(
        carrierCheck(ARRIVAL_START_FRAGMENT),
        payload,
        "'expectedArrivalAtPlaceOfDeliveryStartDate' must not be provided when 'routingReference' is provided.");
    }

    @Test
    void givenRoutingReference_whenArrivalStartDateIsAbsent_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"routingReference":"RR"}
        """);

      assertIrrelevant(carrierCheck(ARRIVAL_START_FRAGMENT), payload);
    }

    @Test
    void givenNoIdentification_whenArrivalStartDateIsMissing_thenErrorIsReported() {
      assertErrorReported(
        carrierCheck(ARRIVAL_START_FRAGMENT),
        emptyBody(),
        "'expectedArrivalAtPlaceOfDeliveryStartDate' must be provided when vessel/voyage/service details or 'expectedDepartureDate' are not provided");
    }

    @Test
    void givenDepartureDate_whenArrivalStartDateIsMissing_thenCheckPasses() {
      JsonNode payload = body("""
        {"expectedDepartureDate":"2026-08-01"}
        """);

      assertConformant(carrierCheck(ARRIVAL_START_FRAGMENT), payload);
    }

    @Test
    void givenVoyageIdentification_whenArrivalStartDateIsMissing_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierExportVoyageNumber":"2103N","carrierServiceCode":"BW1"}
          """);

      assertConformant(carrierCheck(ARRIVAL_START_FRAGMENT), payload);
    }

    @Test
    void givenNoIdentification_whenArrivalEndDateIsMissing_thenErrorIsReported() {
      assertErrorReported(
        carrierCheck(ARRIVAL_END_FRAGMENT),
        emptyBody(),
        "'expectedArrivalAtPlaceOfDeliveryEndDate' must be provided when vessel/voyage/service details or 'expectedDepartureDate' are not provided");
    }

    @Test
    void givenArrivalEndDate_whenProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"expectedArrivalAtPlaceOfDeliveryEndDate":"2026-08-05"}
          """);

      assertConformant(carrierCheck(ARRIVAL_END_FRAGMENT), payload);
    }

    @Test
    void givenRoutingReference_whenArrivalEndDateIsPresent_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"routingReference":"RR","expectedArrivalAtPlaceOfDeliveryEndDate":"2026-08-05"}
            """);

      assertErrorReported(
        carrierCheck(ARRIVAL_END_FRAGMENT),
        payload,
        "'expectedArrivalAtPlaceOfDeliveryEndDate' must not be provided when 'routingReference' is provided.");
    }
  }

  @Nested
  @DisplayName("Routing reference prohibitions")
  class RoutingReferenceProhibitions {

    private static final String EXPORT_VOYAGE_FRAGMENT =
      "The 'universalExportVoyageReference' attribute must not be provided when prohibited";
    private static final String SERVICE_REFERENCE_FRAGMENT =
      "The 'universalServiceReference' attribute must not be provided when prohibited";
    private static final String SHIPMENT_LOCATIONS_FRAGMENT =
      "must not be provided when prohibited by the standard: in case 'routingReference'";

    @Test
    void givenNoRoutingReference_whenExportVoyageProhibitionIsChecked_thenCheckIsIrrelevant() {
      assertIrrelevant(carrierCheck(EXPORT_VOYAGE_FRAGMENT), emptyBody());
    }

    @Test
    void givenRoutingReference_whenExportVoyageReferenceIsPresent_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"routingReference":"RR","universalExportVoyageReference":"2103N"}
            """);

      assertErrorReported(
        carrierCheck(EXPORT_VOYAGE_FRAGMENT),
        payload,
        "'universalExportVoyageReference' must not be provided when 'routingReference' is provided.");
    }

    @Test
    void givenRoutingReference_whenExportVoyageReferenceIsAbsent_thenCheckPasses() {
      JsonNode payload = body("""
        {"routingReference":"RR"}
        """);

      assertConformant(carrierCheck(EXPORT_VOYAGE_FRAGMENT), payload);
    }

    @Test
    void givenNoRoutingReference_whenServiceReferenceProhibitionIsChecked_thenCheckIsIrrelevant() {
      assertIrrelevant(carrierCheck(SERVICE_REFERENCE_FRAGMENT), emptyBody());
    }

    @Test
    void givenRoutingReference_whenServiceReferenceIsPresent_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"routingReference":"RR","universalServiceReference":"SR12345A"}
          """);

      assertErrorReported(
        carrierCheck(SERVICE_REFERENCE_FRAGMENT),
        payload,
        "'universalServiceReference' must not be provided when 'routingReference' is provided.");
    }

    @Test
    void givenNoRoutingReference_whenShipmentLocationProhibitionIsChecked_thenCheckIsIrrelevant() {
      assertIrrelevant(carrierCheck(SHIPMENT_LOCATIONS_FRAGMENT), emptyBody());
    }

    @Test
    void givenRoutingReference_whenPlaceOfReceiptIsProvided_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"routingReference":"RR","shipmentLocations":[{"locationTypeCode":"PRE"}]}
            """);

      assertErrorReported(
        carrierCheck(SHIPMENT_LOCATIONS_FRAGMENT),
        payload,
        "'shipmentLocations.locationTypeCode' 'PRE' must not be provided when 'routingReference' is provided");
    }

    @Test
    void givenRoutingReference_whenNoProhibitedLocationsProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"routingReference":"RR","shipmentLocations":[{"locationTypeCode":"PCF"}]}
            """);

      assertConformant(carrierCheck(SHIPMENT_LOCATIONS_FRAGMENT), payload);
    }
  }

  @Nested
  @DisplayName("Shipment locations")
  class ShipmentLocations {

    private static final String LOCATIONS_FRAGMENT =
      "a Port of Discharge (PDE/POD) and a Port of Load";
    private static final String PORT_OF_LOAD_FRAGMENT = "at least one Port of Load MUST be provided";
    private static final String PORT_OF_DISCHARGE_FRAGMENT =
      "at least one Port of Discharge MUST be provided";

    @Test
    void givenRoutingReferenceWithoutStoreDoor_whenLocationsAreChecked_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"routingReference":"RR","receiptTypeAtOrigin":"CY"}
        """);

      assertIrrelevant(carrierCheck(LOCATIONS_FRAGMENT), payload);
    }

    @Test
    void givenNoRoutingReference_whenPortOfDischargeIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"shipmentLocations":[{"locationTypeCode":"POL"}]}
          """);

      assertErrorReported(
        carrierCheck(LOCATIONS_FRAGMENT),
        payload,
        "Port of Discharge value must be provided (PDE or POD)");
    }

    @Test
    void givenNoRoutingReference_whenPortOfLoadIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"shipmentLocations":[{"locationTypeCode":"POD"}]}
          """);

      assertErrorReported(
        carrierCheck(LOCATIONS_FRAGMENT),
        payload,
        "Port of Load value must be provided (PRE or POL)");
    }

    @Test
    void givenStoreDoorDelivery_whenPlaceOfDeliveryIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"deliveryTypeAtDestination":"SD","shipmentLocations":[{"locationTypeCode":"POL"},{"locationTypeCode":"POD"}]}
            """);

      assertErrorReported(
        carrierCheck(LOCATIONS_FRAGMENT),
        payload,
        "Place of Delivery value must be provided (PDE) when 'deliveryTypeAtDestination' is 'SD'");
    }

    @Test
    void givenStoreDoorReceipt_whenPlaceOfReceiptIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"SD","shipmentLocations":[{"locationTypeCode":"POL"},{"locationTypeCode":"POD"}]}
            """);

      assertErrorReported(
        carrierCheck(LOCATIONS_FRAGMENT),
        payload,
        "Place of Receipt value must be provided (PRE) when 'receiptTypeAtOrigin' is 'SD'");
    }

    @Test
    void givenStoreDoorReceipt_whenContainerPositioningDateTimeIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"SD","shipmentLocations":[{"locationTypeCode":"PRE"},{"locationTypeCode":"PDE"}],"requestedEquipments":[{"containerPositionings":[{}]}]}
            """);

      assertErrorReported(
        carrierCheck(LOCATIONS_FRAGMENT),
        payload,
        "When 'receiptTypeAtOrigin' is 'SD' (Store Door), 'requestedEquipments.containerPositionings.dateTime' is required");
    }

    @Test
    void givenStoreDoorReceipt_whenContainerPositioningDateTimeIsProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"SD","shipmentLocations":[{"locationTypeCode":"PRE"},{"locationTypeCode":"PDE"}],"requestedEquipments":[{"containerPositionings":[{"dateTime":"2026-08-01T00:00:00Z"}]}]}
            """);

      assertConformant(carrierCheck(LOCATIONS_FRAGMENT), payload);
    }

    @Test
    void givenRoutingReferenceWithStoreDoor_whenPositioningDateTimeIsProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"routingReference":"RR","receiptTypeAtOrigin":"SD","requestedEquipments":[{"containerPositionings":[{"dateTime":"2026-08-01T00:00:00Z"}]}]}
            """);

      assertConformant(carrierCheck(LOCATIONS_FRAGMENT), payload);
    }

    @Test
    void givenRoutingReference_whenPortOfLoadRequirementIsChecked_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"routingReference":"RR"}
        """);

      assertIrrelevant(carrierCheck(PORT_OF_LOAD_FRAGMENT), payload);
    }

    @Test
    void givenNoRoutingReference_whenNoPortOfLoadProvided_thenErrorIsReported() {
      assertErrorReported(
        carrierCheck(PORT_OF_LOAD_FRAGMENT),
        emptyBody(),
        "Port of Load value must be provided (PRE or POL)");
    }

    @Test
    void givenNoRoutingReference_whenPlaceOfReceiptProvided_thenPortOfLoadCheckPasses() {
      JsonNode payload =
        body("""
          {"shipmentLocations":[{"locationTypeCode":"PRE"}]}
          """);

      assertConformant(carrierCheck(PORT_OF_LOAD_FRAGMENT), payload);
    }

    @Test
    void givenNoRoutingReference_whenOnlyPortOfLoadProvided_thenPortOfLoadCheckPasses() {
      JsonNode payload =
        body(
          """
            {"shipmentLocations":[{"locationTypeCode":"PCF"},{"locationTypeCode":"POL"}]}
            """);

      assertConformant(carrierCheck(PORT_OF_LOAD_FRAGMENT), payload);
    }

    @Test
    void givenRoutingReference_whenPortOfDischargeRequirementIsChecked_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"routingReference":"RR"}
        """);

      assertIrrelevant(carrierCheck(PORT_OF_DISCHARGE_FRAGMENT), payload);
    }

    @Test
    void givenNoRoutingReference_whenNoPortOfDischargeProvided_thenErrorIsReported() {
      assertErrorReported(
        carrierCheck(PORT_OF_DISCHARGE_FRAGMENT),
        emptyBody(),
        "Port of Discharge value must be provided (PDE or POD)");
    }

    @Test
    void givenNoRoutingReference_whenPlaceOfDeliveryProvided_thenPortOfDischargeCheckPasses() {
      JsonNode payload =
        body("""
          {"shipmentLocations":[{"locationTypeCode":"PDE"}]}
          """);

      assertConformant(carrierCheck(PORT_OF_DISCHARGE_FRAGMENT), payload);
    }

    @Test
    void givenNoRoutingReference_whenOnlyPortOfDischargeProvided_thenPortOfDischargeCheckPasses() {
      JsonNode payload =
        body(
          """
            {"shipmentLocations":[{"locationTypeCode":"PCF"},{"locationTypeCode":"POD"}]}
            """);

      assertConformant(carrierCheck(PORT_OF_DISCHARGE_FRAGMENT), payload);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Booking status driven validations
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Feedbacks")
  class Feedbacks {

    @Test
    void givenConfirmedBooking_whenFeedbacksAreMissing_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"bookingStatus":"CONFIRMED"}
        """);

      assertIrrelevant(BookingChecks.FEEDBACKS_PRESENCE, payload);
    }

    @Test
    void givenBookingWithoutStatus_whenFeedbacksAreMissing_thenCheckIsIrrelevant() {
      assertIrrelevant(BookingChecks.FEEDBACKS_PRESENCE, emptyBody());
    }

    @Test
    void givenPendingUpdateBooking_whenFeedbacksAreMissing_thenErrorIsReported() {
      JsonNode payload = body("""
        {"bookingStatus":"PENDING_UPDATE"}
        """);

      assertErrorReported(
        BookingChecks.FEEDBACKS_PRESENCE,
        payload,
        "'feedbacks' is missing in the 'bookingStatus' 'PENDING_UPDATE'");
    }

    @Test
    void givenPendingAmendmentBooking_whenFeedbacksAreEmpty_thenErrorIsReported() {
      JsonNode payload = body("""
        {"bookingStatus":"PENDING_AMENDMENT","feedbacks":[]}
        """);

      assertErrorReported(
        BookingChecks.FEEDBACKS_PRESENCE,
        payload,
        "'feedbacks' is missing in the 'bookingStatus' 'PENDING_AMENDMENT'");
    }

    @Test
    void givenPendingUpdateBooking_whenFeedbacksAreProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"bookingStatus":"PENDING_UPDATE","feedbacks":[{"code":"PROPERTY_VALUE_MUST_CHANGE","severity":"ERROR"}]}
            """);

      assertConformant(BookingChecks.FEEDBACKS_PRESENCE, payload);
    }

    @Test
    void givenFeedbackWithInvalidSeverity_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"feedbacks":[{"severity":"CRITICAL"}]}
        """);

      assertNotConformant(BookingChecks.VALID_FEEDBACK_SEVERITY, payload);
    }

    @Test
    void givenFeedbackWithValidSeverity_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"feedbacks":[{"severity":"INFO"}]}
        """);

      assertConformant(BookingChecks.VALID_FEEDBACK_SEVERITY, payload);
    }

    @Test
    void givenFeedbackWithInvalidCode_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"feedbacks":[{"code":"UNKNOWN"}]}
        """);

      assertNotConformant(BookingChecks.VALID_FEEDBACK_CODE, payload);
    }

    @Test
    void givenFeedbackWithValidCode_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"feedbacks":[{"code":"INFORMATIONAL_MESSAGE"}]}
        """);

      assertConformant(BookingChecks.VALID_FEEDBACK_CODE, payload);
    }
  }

  @Nested
  @DisplayName("Attributes required by booking status")
  class AttributesRequiredByBookingStatus {

    private static final String CONFIRMED_EQUIPMENTS_FRAGMENT =
      "The 'confirmedEquipments' attribute must be provided when";
    private static final String TRANSPORT_PLAN_FRAGMENT =
      "The 'transportPlan' attribute must be provided when";
    private static final String CUT_OFF_TIMES_FRAGMENT =
      "The 'shipmentCutOffTimes' attribute must be provided when";
    private static final String CARRIER_BOOKING_REFERENCE_FRAGMENT =
      "except for the booking states where it is still optional";

    @Test
    void givenReceivedBooking_whenConfirmedEquipmentsAreChecked_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"bookingStatus":"RECEIVED"}
        """);

      assertIrrelevant(carrierCheck(CONFIRMED_EQUIPMENTS_FRAGMENT), payload);
    }

    @Test
    void givenConfirmedBooking_whenConfirmedEquipmentsAreMissing_thenErrorIsReported() {
      JsonNode payload = body("""
        {"bookingStatus":"CONFIRMED"}
        """);

      assertErrorReported(
        carrierCheck(CONFIRMED_EQUIPMENTS_FRAGMENT),
        payload,
        "'confirmedEquipments' must be provided for bookingStatus 'CONFIRMED'");
    }

    @Test
    void givenConfirmedBooking_whenConfirmedEquipmentsArePresent_thenCheckPasses() {
      JsonNode payload = body("""
        {"bookingStatus":"CONFIRMED","confirmedEquipments":[{}]}
        """);

      assertConformant(carrierCheck(CONFIRMED_EQUIPMENTS_FRAGMENT), payload);
    }

    @Test
    void givenConfirmedBooking_whenTransportPlanIsMissing_thenErrorIsReported() {
      JsonNode payload = body("""
        {"bookingStatus":"CONFIRMED"}
        """);

      assertErrorReported(
        carrierCheck(TRANSPORT_PLAN_FRAGMENT),
        payload,
        "'transportPlan' must be provided for bookingStatus 'CONFIRMED'");
    }

    @Test
    void givenConfirmedBooking_whenShipmentCutOffTimesAreMissing_thenErrorIsReported() {
      JsonNode payload = body("""
        {"bookingStatus":"CONFIRMED"}
        """);

      assertErrorReported(
        carrierCheck(CUT_OFF_TIMES_FRAGMENT),
        payload,
        "'shipmentCutOffTimes' must be provided for bookingStatus 'CONFIRMED'");
    }

    @Test
    void givenOptionalReferenceState_whenCarrierBookingReferenceIsMissing_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"bookingStatus":"RECEIVED"}
        """);

      assertIrrelevant(carrierCheck(CARRIER_BOOKING_REFERENCE_FRAGMENT), payload);
    }

    @Test
    void givenConfirmedBooking_whenCarrierBookingReferenceIsMissing_thenErrorIsReported() {
      JsonNode payload = body("""
        {"bookingStatus":"CONFIRMED"}
        """);

      assertErrorReported(
        carrierCheck(CARRIER_BOOKING_REFERENCE_FRAGMENT),
        payload,
        "'carrierBookingReference' must be present for booking status 'CONFIRMED'");
    }

    @Test
    void givenConfirmedBooking_whenCarrierBookingReferenceIsPresent_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"bookingStatus":"CONFIRMED","carrierBookingReference":"CBR456"}
          """);

      assertConformant(carrierCheck(CARRIER_BOOKING_REFERENCE_FRAGMENT), payload);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Conditional attribute validations
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Container positioning and pickup")
  class ContainerPositioningAndPickup {

    private static final String REQUESTED_POSITIONINGS_FRAGMENT =
      "The 'requestedEquipments.containerPositionings' attribute must only be used";
    private static final String REQUESTED_POSITIONING_LOCATION_FRAGMENT =
      "The 'requestedEquipments.containerPositionings.location' object must only be used";
    private static final String EMPTY_PICKUP_FRAGMENT =
      "The 'requestedEquipments.emptyContainerPickup' attribute must only be used";
    private static final String CONFIRMED_POSITIONINGS_FRAGMENT =
      "The 'confirmedEquipments.containerPositionings' attribute must only be used";
    private static final String CONFIRMED_ESTIMATED_DATE_TIME_FRAGMENT =
      "The 'confirmedEquipments.containerPositionings.estimatedDateTime' attribute must only be used";

    @Test
    void givenNoContainerPositionings_whenValidated_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"requestedEquipments":[{}]}
        """);

      assertIrrelevant(carrierCheck(REQUESTED_POSITIONINGS_FRAGMENT), payload);
    }

    @Test
    void givenNonStoreDoorReceipt_whenContainerPositioningsProvided_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"CY","requestedEquipments":[{"containerPositionings":[{"dateTime":"2026-08-01T00:00:00Z"}]}]}
            """);

      assertErrorReported(
        carrierCheck(REQUESTED_POSITIONINGS_FRAGMENT),
        payload,
        "'requestedEquipments[0].containerPositionings' must be absent when 'receiptTypeAtOrigin' is not 'SD'");
    }

    @Test
    void givenStoreDoorReceipt_whenContainerPositioningsProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"SD","requestedEquipments":[{"containerPositionings":[{"dateTime":"2026-08-01T00:00:00Z"}]}]}
            """);

      assertConformant(carrierCheck(REQUESTED_POSITIONINGS_FRAGMENT), payload);
    }

    @Test
    void givenNoPositioningLocation_whenValidated_thenCheckIsIrrelevant() {
      JsonNode payload =
        body("""
          {"requestedEquipments":[{"containerPositionings":[{}]}]}
          """);

      assertIrrelevant(carrierCheck(REQUESTED_POSITIONING_LOCATION_FRAGMENT), payload);
    }

    @Test
    void givenNonStoreDoorReceipt_whenPositioningLocationProvided_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"CY","requestedEquipments":[{"containerPositionings":[{"location":{"UNLocationCode":"NLAMS"}}]}]}
            """);

      assertErrorReported(
        carrierCheck(REQUESTED_POSITIONING_LOCATION_FRAGMENT),
        payload,
        "'requestedEquipments[0].containerPositionings[0].location' must be absent when 'receiptTypeAtOrigin' is not 'SD'");
    }

    @Test
    void givenStoreDoorReceipt_whenPositioningLocationProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"SD","requestedEquipments":[{"containerPositionings":[{"location":{"UNLocationCode":"NLAMS"}}]}]}
            """);

      assertConformant(carrierCheck(REQUESTED_POSITIONING_LOCATION_FRAGMENT), payload);
    }

    @Test
    void givenNoEmptyContainerPickup_whenValidated_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"requestedEquipments":[{}]}
        """);

      assertIrrelevant(carrierCheck(EMPTY_PICKUP_FRAGMENT), payload);
    }

    @Test
    void givenCarrierHaulage_whenEmptyContainerPickupProvided_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"SD","requestedEquipments":[{"emptyContainerPickup":{"dateTime":"2026-08-01T00:00:00Z"}}]}
            """);

      assertErrorReported(
        carrierCheck(EMPTY_PICKUP_FRAGMENT),
        payload,
        "'requestedEquipments[0].emptyContainerPickup' must be absent when 'receiptTypeAtOrigin' is not 'CY'");
    }

    @Test
    void givenMerchantHaulage_whenEmptyContainerPickupProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"CY","requestedEquipments":[{"emptyContainerPickup":{"dateTime":"2026-08-01T00:00:00Z"}}]}
            """);

      assertConformant(carrierCheck(EMPTY_PICKUP_FRAGMENT), payload);
    }

    @Test
    void givenNoConfirmedPositionings_whenValidated_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"confirmedEquipments":[{}]}
        """);

      assertIrrelevant(carrierCheck(CONFIRMED_POSITIONINGS_FRAGMENT), payload);
    }

    @Test
    void givenNonStoreDoorReceipt_whenConfirmedPositioningsProvided_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"CY","confirmedEquipments":[{"containerPositionings":[{"estimatedDateTime":"2026-08-01T00:00:00Z"}]}]}
            """);

      assertErrorReported(
        carrierCheck(CONFIRMED_POSITIONINGS_FRAGMENT),
        payload,
        "'confirmedEquipments[0].containerPositionings' must be absent when 'receiptTypeAtOrigin' is not 'SD'");
    }

    @Test
    void givenStoreDoorReceipt_whenConfirmedPositioningsProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"SD","confirmedEquipments":[{"containerPositionings":[{"estimatedDateTime":"2026-08-01T00:00:00Z"}]}]}
            """);

      assertConformant(carrierCheck(CONFIRMED_POSITIONINGS_FRAGMENT), payload);
    }

    @Test
    void givenNoConfirmedEstimatedDateTime_whenValidated_thenCheckIsIrrelevant() {
      JsonNode payload =
        body("""
          {"confirmedEquipments":[{"containerPositionings":[{}]}]}
          """);

      assertIrrelevant(carrierCheck(CONFIRMED_ESTIMATED_DATE_TIME_FRAGMENT), payload);
    }

    @Test
    void givenNonStoreDoorReceipt_whenConfirmedEstimatedDateTimeProvided_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"CY","confirmedEquipments":[{"containerPositionings":[{"estimatedDateTime":"2026-08-01T00:00:00Z"}]}]}
            """);

      assertErrorReported(
        carrierCheck(CONFIRMED_ESTIMATED_DATE_TIME_FRAGMENT),
        payload,
        "'confirmedEquipments[0].containerPositionings[0].estimatedDateTime' must be absent when 'receiptTypeAtOrigin' is not 'SD'");
    }

    @Test
    void givenStoreDoorReceipt_whenConfirmedEstimatedDateTimeProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"SD","confirmedEquipments":[{"containerPositionings":[{"estimatedDateTime":"2026-08-01T00:00:00Z"}]}]}
            """);

      assertConformant(carrierCheck(CONFIRMED_ESTIMATED_DATE_TIME_FRAGMENT), payload);
    }
  }

  @Nested
  @DisplayName("Shipper owned containers")
  class ShipperOwnedContainers {

    private static final String FRAGMENT = "Shipper Owned Containers (SOC)";

    @Test
    void givenCarrierOwnedContainer_whenTareWeightIsMissing_thenCheckIsIrrelevant() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"isShipperOwned":false}]}
        """);

      assertIrrelevant(carrierCheck(FRAGMENT), payload);
    }

    @Test
    void givenShipperOwnedContainer_whenTareWeightIsMissing_thenErrorIsReported() {
      JsonNode payload = body("""
        {"requestedEquipments":[{"isShipperOwned":true}]}
        """);

      assertErrorReported(
        carrierCheck(FRAGMENT),
        payload,
        "'requestedEquipments[0].tareWeight' is required when 'isShipperOwned' is 'true' (Shipper Owned Container)");
    }

    @Test
    void givenShipperOwnedContainer_whenTareWeightIsProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"isShipperOwned":true,"tareWeight":{"value":2000,"unit":"KGM"}}]}
            """);

      assertConformant(carrierCheck(FRAGMENT), payload);
    }
  }

  @Nested
  @DisplayName("Document parties")
  class DocumentParties {

    private static final String SEND_TO_PLATFORM_FRAGMENT =
      "The 'documentParties.issueTo.sendToPlatform' attribute must only be used";
    private static final String PLACE_OF_BL_ISSUE_FRAGMENT = "The 'placeOfBLIssue' object must";
    private static final String CONTACT_DETAILS_FRAGMENT = "The 'partyContactDetails' object must";
    private static final String OTHER_PARTY_FUNCTION_FRAGMENT = "other document party function code";
    private static final String CODE_LIST_PROVIDER_FRAGMENT = "code list provider code";

    @Test
    void givenNoSendToPlatform_whenValidated_thenCheckIsIrrelevant() {
      assertIrrelevant(carrierCheck(SEND_TO_PLATFORM_FRAGMENT), emptyBody());
    }

    @Test
    void givenSendToPlatform_whenTransportDocumentTypeIsNotBillOfLading_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"transportDocumentTypeCode":"SWB","isElectronic":true,"documentParties":{"issueTo":{"sendToPlatform":"WAVE"}}}
            """);

      assertErrorReported(
        carrierCheck(SEND_TO_PLATFORM_FRAGMENT),
        payload,
        "'documentParties.issueTo.sendToPlatform' is only allowed when 'transportDocumentTypeCode' is 'BOL'");
    }

    @Test
    void givenSendToPlatform_whenDocumentIsNotElectronic_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"transportDocumentTypeCode":"BOL","isElectronic":false,"documentParties":{"issueTo":{"sendToPlatform":"WAVE"}}}
            """);

      assertErrorReported(
        carrierCheck(SEND_TO_PLATFORM_FRAGMENT),
        payload,
        "'documentParties.issueTo.sendToPlatform' must be absent for paper B/Ls ('isElectronic' is 'false')");
    }

    @Test
    void givenSendToPlatform_whenElectronicBillOfLading_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"transportDocumentTypeCode":"BOL","isElectronic":true,"documentParties":{"issueTo":{"sendToPlatform":"WAVE"}}}
            """);

      assertConformant(carrierCheck(SEND_TO_PLATFORM_FRAGMENT), payload);
    }

    @Test
    void givenSendToPlatform_whenElectronicFlagIsMissing_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"transportDocumentTypeCode":"BOL","documentParties":{"issueTo":{"sendToPlatform":"WAVE"}}}
            """);

      assertConformant(carrierCheck(SEND_TO_PLATFORM_FRAGMENT), payload);
    }

    @Test
    void givenNoPlaceOfBlIssue_whenValidated_thenCheckIsIrrelevant() {
      assertIrrelevant(carrierCheck(PLACE_OF_BL_ISSUE_FRAGMENT), emptyBody());
    }

    @Test
    void givenPlaceOfBlIssue_whenBothLocationIdentifiersProvided_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"placeOfBLIssue":{"UNLocationCode":"NLAMS","countryCode":"NL"}}
            """);

      assertErrorReported(
        carrierCheck(PLACE_OF_BL_ISSUE_FRAGMENT),
        payload,
        "'placeOfBLIssue' must contain exactly one of 'UNLocationCode' or 'countryCode', but not both");
    }

    @Test
    void givenPlaceOfBlIssue_whenNoLocationIdentifierProvided_thenErrorIsReported() {
      JsonNode payload = body("""
        {"placeOfBLIssue":{"locationName":"Amsterdam"}}
        """);

      assertErrorReported(
        carrierCheck(PLACE_OF_BL_ISSUE_FRAGMENT),
        payload,
        "'placeOfBLIssue' must contain exactly one of 'UNLocationCode' or 'countryCode', but not both");
    }

    @Test
    void givenPlaceOfBlIssue_whenOnlyUnLocationCodeProvided_thenCheckPasses() {
      JsonNode payload = body("""
        {"placeOfBLIssue":{"UNLocationCode":"NLAMS"}}
        """);

      assertConformant(carrierCheck(PLACE_OF_BL_ISSUE_FRAGMENT), payload);
    }

    @Test
    void givenContactDetails_whenNameIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"partyContactDetails":[{"email":"a@b.com"}]}
          """);

      assertErrorReported(
        carrierCheck(CONTACT_DETAILS_FRAGMENT),
        payload,
        "'partyContactDetails[0].name' is mandatory");
    }

    @Test
    void givenContactDetails_whenPhoneAndEmailAreMissing_thenErrorIsReported() {
      JsonNode payload = body("""
        {"partyContactDetails":[{"name":"Jane"}]}
        """);

      assertErrorReported(
        carrierCheck(CONTACT_DETAILS_FRAGMENT),
        payload,
        "'partyContactDetails[0]' must provide either 'phone' and/or 'email'");
    }

    @Test
    void givenContactDetails_whenNameAndPhoneProvided_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"partyContactDetails":[{"name":"Jane","phone":"+31000000"}]}
          """);

      assertConformant(carrierCheck(CONTACT_DETAILS_FRAGMENT), payload);
    }

    @Test
    void givenOtherPartyWithInvalidFunction_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload =
        body("""
          {"documentParties":{"other":[{"partyFunction":"XXX"}]}}
          """);

      assertNotConformant(carrierCheck(OTHER_PARTY_FUNCTION_FRAGMENT), payload);
    }

    @Test
    void givenOtherPartyWithValidFunction_whenValidated_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"documentParties":{"other":[{"partyFunction":"DDR"}]}}
          """);

      assertConformant(carrierCheck(OTHER_PARTY_FUNCTION_FRAGMENT), payload);
    }

    @Test
    void givenShipperWithInvalidCodeListProvider_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload =
        body(
          """
            {"documentParties":{"shipper":{"identifyingCodes":[{"codeListProvider":"NOPE"}]}}}
            """);

      assertNotConformant(carrierCheck(CODE_LIST_PROVIDER_FRAGMENT), payload);
    }

    @Test
    void givenOtherPartyWithValidCodeListProvider_whenValidated_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"documentParties":{"other":[{"party":{"identifyingCodes":[{"codeListProvider":"DCSA"}]}}]}}
            """);

      assertConformant(carrierCheck(CODE_LIST_PROVIDER_FRAGMENT), payload);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Dataset driven validations
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Dataset validations")
  class DatasetValidations {

    private static final String NATIONAL_COMMODITY_FRAGMENT = "in each 'nationalCommodityCodes' object";
    private static final String EXTENDED_NATIONAL_COMMODITY_FRAGMENT =
      "in each 'extendedNationalCommodityCodes' object";
    private static final String CARRIER_REFERENCE_FRAGMENT =
      "in each reference object in the Booking response";
    private static final String CUT_OFF_CODE_FRAGMENT = "correct use of a cut-off time code";
    private static final String LOCATION_TYPE_FRAGMENT = "shipment location type code";
    private static final String TRANSPORT_PLAN_MODE_FRAGMENT =
      "The 'transportPlan.modeOfTransport' attribute";
    private static final String INHALATION_FRAGMENT = "inhalation hazard zone";

    @Test
    void givenInvalidNationalCommodityType_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"nationalCommodityCodes":[{"type":"XXX"}]}]}]}
            """);

      assertNotConformant(carrierCheck(NATIONAL_COMMODITY_FRAGMENT), payload);
    }

    @Test
    void givenValidNationalCommodityType_whenValidated_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"nationalCommodityCodes":[{"type":"NCM"}]}]}]}
            """);

      assertConformant(carrierCheck(NATIONAL_COMMODITY_FRAGMENT), payload);
    }

    @Test
    void givenValidExtendedNationalCommodityType_whenValidated_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"extendedNationalCommodityCodes":[{"type":"NCM"}]}]}]}
            """);

      assertConformant(carrierCheck(EXTENDED_NATIONAL_COMMODITY_FRAGMENT), payload);
    }

    @Test
    void givenInvalidExtendedNationalCommodityType_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"extendedNationalCommodityCodes":[{"type":"XXX"}]}]}]}
            """);

      assertNotConformant(carrierCheck(EXTENDED_NATIONAL_COMMODITY_FRAGMENT), payload);
    }

    @Test
    void givenInvalidShipperReferenceType_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"references":[{"type":"ECR"}]}
        """);

      assertNotConformant(BookingChecks.SHIPPER_REFERENCE_TYPE_VALIDATION, payload);
    }

    @Test
    void givenValidShipperReferenceType_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"references":[{"type":"CR"}]}
        """);

      assertConformant(BookingChecks.SHIPPER_REFERENCE_TYPE_VALIDATION, payload);
    }

    @Test
    void givenValidCarrierReferenceTypeOnEquipment_whenValidated_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"requestedEquipments":[{"references":[{"type":"ECR"}]}]}
          """);

      assertConformant(carrierCheck(CARRIER_REFERENCE_FRAGMENT), payload);
    }

    @Test
    void givenInvalidCarrierReferenceTypeOnCommodity_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"references":[{"type":"XXX"}]}]}]}
            """);

      assertNotConformant(carrierCheck(CARRIER_REFERENCE_FRAGMENT), payload);
    }

    @Test
    void givenInvalidCutOffDateTimeCode_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"shipmentCutOffTimes":[{"cutOffDateTimeCode":"XXX"}]}
        """);

      assertNotConformant(carrierCheck(CUT_OFF_CODE_FRAGMENT), payload);
    }

    @Test
    void givenValidCutOffDateTimeCode_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"shipmentCutOffTimes":[{"cutOffDateTimeCode":"DCO"}]}
        """);

      assertConformant(carrierCheck(CUT_OFF_CODE_FRAGMENT), payload);
    }

    @Test
    void givenInvalidShipmentLocationType_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"shipmentLocations":[{"locationTypeCode":"XXX"}]}
        """);

      assertNotConformant(carrierCheck(LOCATION_TYPE_FRAGMENT), payload);
    }

    @Test
    void givenValidShipmentLocationType_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"shipmentLocations":[{"locationTypeCode":"POL"}]}
        """);

      assertConformant(carrierCheck(LOCATION_TYPE_FRAGMENT), payload);
    }

    @Test
    void givenInvalidRequestedPreCarriageMode_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"requestedPreCarriageModeOfTransport":"PLANE"}
        """);

      assertNotConformant(BookingChecks.REQUESTED_CARRIAGE_MODE_OF_TRANSPORT_VALIDATION, payload);
    }

    @Test
    void givenValidRequestedOnCarriageMode_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"requestedOnCarriageModeOfTransport":"RAIL"}
        """);

      assertConformant(BookingChecks.REQUESTED_CARRIAGE_MODE_OF_TRANSPORT_VALIDATION, payload);
    }

    @Test
    void givenInvalidTransportPlanMode_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"transportPlan":[{"modeOfTransport":"PLANE"}]}
        """);

      assertNotConformant(carrierCheck(TRANSPORT_PLAN_MODE_FRAGMENT), payload);
    }

    @Test
    void givenValidTransportPlanMode_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"transportPlan":[{"modeOfTransport":"VESSEL"}]}
        """);

      assertConformant(carrierCheck(TRANSPORT_PLAN_MODE_FRAGMENT), payload);
    }

    @Test
    void givenInvalidCargoMovementTypeAtOrigin_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"cargoMovementTypeAtOrigin":"XXX"}
        """);

      assertNotConformant(carrierCheck("The 'cargoMovementTypeAtOrigin' attribute"), payload);
    }

    @Test
    void givenValidCargoMovementTypeAtDestination_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"cargoMovementTypeAtDestination":"FCL"}
        """);

      assertConformant(carrierCheck("The 'cargoMovementTypeAtDestination' attribute"), payload);
    }

    @Test
    void givenInvalidBookingStatus_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"bookingStatus":"UNKNOWN"}
        """);

      assertNotConformant(carrierCheck("correct use of a booking status code"), payload);
    }

    @Test
    void givenInvalidAmendedBookingStatus_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"amendedBookingStatus":"UNKNOWN"}
        """);

      assertNotConformant(carrierCheck("amended booking status code"), payload);
    }

    @Test
    void givenValidBookingCancellationStatus_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"bookingCancellationStatus":"CANCELLATION_RECEIVED"}
        """);

      assertConformant(carrierCheck("booking cancellation status code"), payload);
    }

    @Test
    void givenSegregationGroupWithinRange_whenValidated_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[{"segregationGroups":["1","18"]}]}}]}]}
            """);

      assertConformant(BookingChecks.DG_SEGREGATION_GROUP_CODE_VALIDATION, payload);
    }

    @Test
    void givenSegregationGroupOutsideRange_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[{"segregationGroups":["19"]}]}}]}]}
            """);

      assertNotConformant(BookingChecks.DG_SEGREGATION_GROUP_CODE_VALIDATION, payload);
    }

    @Test
    void givenInvalidInhalationZone_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[{"inhalationZone":["Z"]}]}}]}]}
            """);

      assertNotConformant(carrierCheck(INHALATION_FRAGMENT), payload);
    }

    @Test
    void givenValidInhalationZone_whenValidated_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[{"inhalationZone":["A"]}]}}]}]}
            """);

      assertConformant(carrierCheck(INHALATION_FRAGMENT), payload);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Numeric and structural validations
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Numeric validations")
  class NumericValidations {

    private static final String INNER_PACKAGING_FRAGMENT =
      "The 'innerPackagings.quantity' attribute must be a positive integer";
    private static final String SEQUENCE_NUMBER_FRAGMENT =
      "The 'transportPlan.transportPlanStageSequenceNumber' attribute must be a positive integer";
    private static final String MONETARY_FRAGMENT =
      "monetary amount with no more than two decimal places";

    @Test
    void givenPositiveInnerPackagingQuantity_whenValidated_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[{"innerPackagings":[{"quantity":2}]}]}}]}]}
            """);

      assertConformant(carrierCheck(INNER_PACKAGING_FRAGMENT), payload);
    }

    @Test
    void givenZeroInnerPackagingQuantity_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[{"innerPackagings":[{"quantity":0}]}]}}]}]}
            """);

      assertNotConformant(carrierCheck(INNER_PACKAGING_FRAGMENT), payload);
    }

    @Test
    void givenNonIntegerInnerPackagingQuantity_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[{"innerPackagings":[{"quantity":"two"}]}]}}]}]}
            """);

      assertNotConformant(carrierCheck(INNER_PACKAGING_FRAGMENT), payload);
    }

    @Test
    void givenPositiveTransportPlanSequenceNumber_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"transportPlan":[{"transportPlanStageSequenceNumber":1}]}
        """);

      assertConformant(carrierCheck(SEQUENCE_NUMBER_FRAGMENT), payload);
    }

    @Test
    void givenNegativeTransportPlanSequenceNumber_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"transportPlan":[{"transportPlanStageSequenceNumber":-1}]}
        """);

      assertNotConformant(carrierCheck(SEQUENCE_NUMBER_FRAGMENT), payload);
    }

    @Test
    void givenNonNumericCurrencyAmount_whenValidated_thenErrorIsReported() {
      JsonNode payload = body("""
        {"charges":[{"currencyAmount":"ten"}]}
        """);

      assertErrorReported(
        carrierCheck(MONETARY_FRAGMENT), payload, "charges[0].currencyAmount must be a number");
    }

    @Test
    void givenCurrencyAmountWithThreeDecimals_whenValidated_thenErrorIsReported() {
      JsonNode payload = body("""
        {"charges":[{"currencyAmount":10.123}]}
        """);

      assertErrorReported(
        carrierCheck(MONETARY_FRAGMENT),
        payload,
        "charges[0].currencyAmount must have at most 2 decimal places of precision");
    }

    @Test
    void givenCurrencyAmountWithTwoDecimals_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"charges":[{"currencyAmount":10.12}]}
        """);

      assertConformant(carrierCheck(MONETARY_FRAGMENT), payload);
    }

    @Test
    void givenNotANumberCurrencyAmount_whenValidated_thenErrorIsReported() {
      ObjectNode charge = OBJECT_MAPPER.createObjectNode();
      charge.put("currencyAmount", Double.NaN);
      ObjectNode payload = emptyBody();
      payload.set("charges", OBJECT_MAPPER.createArrayNode().add(charge));

      assertErrorReported(
        carrierCheck(MONETARY_FRAGMENT),
        payload,
        "charges[0].currencyAmount must be a valid decimal number");
    }
  }

  @Nested
  @DisplayName("Structural validations")
  class StructuralValidations {

    private static final String ADVANCE_MANIFEST_FRAGMENT =
      "the combination of 'countryCode' and 'manifestTypeCode' MUST be unique";
    private static final String NUMBER_OF_PACKAGES_FRAGMENT =
      "in case this OuterPackaging includes Dangerous Goods";
    private static final String DECLARED_VALUE_FRAGMENT =
      "The 'declaredValueCurrency' attribute must be provided when";
    private static final String CONTRACT_REFERENCE_FRAGMENT =
      "'contractQuotationReference' / 'serviceContractReference'";
    private static final String AT_LEAST_ONE_REFERENCE_FRAGMENT = "by providing at least one of them";

    @Test
    void givenUniqueAdvanceManifestFilings_whenValidated_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"advanceManifestFilings":[{"countryCode":"NL","manifestTypeCode":"ENS"},{"countryCode":"US","manifestTypeCode":"ENS"}]}
            """);

      assertConformant(carrierCheck(ADVANCE_MANIFEST_FRAGMENT), payload);
    }

    @Test
    void givenDuplicateAdvanceManifestFilings_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload =
        body(
          """
            {"advanceManifestFilings":[{"countryCode":"NL","manifestTypeCode":"ENS"},{"countryCode":"NL","manifestTypeCode":"ENS"}]}
            """);

      assertNotConformant(carrierCheck(ADVANCE_MANIFEST_FRAGMENT), payload);
    }

    @Test
    void givenOuterPackagingWithoutDangerousGoods_whenValidated_thenCheckIsIrrelevant() {
      JsonNode payload =
        body("""
          {"requestedEquipments":[{"commodities":[{"outerPackaging":{}}]}]}
          """);

      assertIrrelevant(carrierCheck(NUMBER_OF_PACKAGES_FRAGMENT), payload);
    }

    @Test
    void givenDangerousGoods_whenNumberOfPackagesIsMissing_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"dangerousGoods":[{}]}}]}]}
            """);

      assertErrorReported(
        carrierCheck(NUMBER_OF_PACKAGES_FRAGMENT),
        payload,
        "The 'requestedEquipments[0].commodities[0].outerPackaging' object did not have a 'numberOfPackages', which is required due to dangerousGoods");
    }

    @Test
    void givenDangerousGoods_whenNumberOfPackagesIsProvided_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"requestedEquipments":[{"commodities":[{"outerPackaging":{"numberOfPackages":2,"dangerousGoods":[{}]}}]}]}
            """);

      assertConformant(carrierCheck(NUMBER_OF_PACKAGES_FRAGMENT), payload);
    }

    @Test
    void givenDeclaredValueWithoutCurrency_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload = body("""
        {"declaredValue":100}
        """);

      assertNotConformant(carrierCheck(DECLARED_VALUE_FRAGMENT), payload);
    }

    @Test
    void givenDeclaredValueWithCurrency_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"declaredValue":100,"declaredValueCurrency":"EUR"}
        """);

      assertConformant(carrierCheck(DECLARED_VALUE_FRAGMENT), payload);
    }

    @Test
    void givenBothContractReferences_whenValidated_thenCheckIsNotConformant() {
      JsonNode payload =
        body("""
          {"contractQuotationReference":"CQR","serviceContractReference":"SCR"}
          """);

      assertNotConformant(carrierCheck(CONTRACT_REFERENCE_FRAGMENT), payload);
    }

    @Test
    void givenSingleContractReference_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"serviceContractReference":"SCR"}
        """);

      assertConformant(carrierCheck(CONTRACT_REFERENCE_FRAGMENT), payload);
    }

    @Test
    void givenNoCarrierReferences_whenValidated_thenErrorIsReported() {
      assertErrorReported(
        carrierCheck(AT_LEAST_ONE_REFERENCE_FRAGMENT),
        emptyBody(),
        "At least one of 'carrierBookingRequestReference' or 'carrierBookingReference' must be present");
    }

    @Test
    void givenCarrierBookingReference_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"carrierBookingReference":"CBR456"}
        """);

      assertConformant(carrierCheck(AT_LEAST_ONE_REFERENCE_FRAGMENT), payload);
    }

    @Test
    void givenCarrierBookingRequestReference_whenValidated_thenCheckPasses() {
      JsonNode payload = body("""
        {"carrierBookingRequestReference":"CBRR123"}
        """);

      assertConformant(carrierCheck(AT_LEAST_ONE_REFERENCE_FRAGMENT), payload);
    }

    @Test
    void givenLateCutOffWithoutContainerFreightStation_whenValidated_thenErrorIsReported() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"CY","shipmentCutOffTimes":[{"cutOffDateTimeCode":"LCO"}]}
            """);

      assertErrorReported(
        BookingChecks.VALIDATE_SHIPMENT_CUTOFF_TIME_CODE,
        payload,
        "'shipmentCutOffTimes.cutOffDateTimeCode' value 'LCO' must only be used when 'receiptTypeAtOrigin' is 'CFS'");
    }

    @Test
    void givenLateCutOffWithContainerFreightStation_whenValidated_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"CFS","shipmentCutOffTimes":[{"cutOffDateTimeCode":"LCO"}]}
            """);

      assertConformant(BookingChecks.VALIDATE_SHIPMENT_CUTOFF_TIME_CODE, payload);
    }

    @Test
    void givenNoLateCutOff_whenValidated_thenCheckIsIrrelevant() {
      JsonNode payload =
        body(
          """
            {"receiptTypeAtOrigin":"CFS","shipmentCutOffTimes":[{"cutOffDateTimeCode":"EFC"}]}
            """);

      assertIrrelevant(BookingChecks.VALIDATE_SHIPMENT_CUTOFF_TIME_CODE, payload);
    }

    @Test
    void givenNonTextualCutOffCode_whenValidated_thenCheckIsIrrelevant() {
      JsonNode payload =
        body("""
          {"receiptTypeAtOrigin":"CY","shipmentCutOffTimes":[{"cutOffDateTimeCode":1}]}
          """);

      assertIrrelevant(BookingChecks.VALIDATE_SHIPMENT_CUTOFF_TIME_CODE, payload);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Scenario reference validations
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("Scenario reference validations")
  class ScenarioReferenceValidations {

    @Test
    void givenBothReferencesMatching_whenValidatingEitherReference_thenCheckPasses() {
      JsonNode payload =
        body(
          """
            {"carrierBookingRequestReference":"CBRR123","carrierBookingReference":"CBR456"}
            """);

      assertConformant(BookingChecks.cbrrOrCbr(DRY_CARGO_PARAMETERS), payload);
    }

    @Test
    void givenMatchingRequestReference_whenValidatingEitherReference_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierBookingRequestReference":"CBRR123","carrierBookingReference":"WRONG"}
          """);

      assertConformant(BookingChecks.cbrrOrCbr(DRY_CARGO_PARAMETERS), payload);
    }

    @Test
    void givenMatchingBookingReference_whenValidatingEitherReference_thenCheckPasses() {
      JsonNode payload =
        body("""
          {"carrierBookingRequestReference":"WRONG","carrierBookingReference":"CBR456"}
          """);

      assertConformant(BookingChecks.cbrrOrCbr(DRY_CARGO_PARAMETERS), payload);
    }

    @Test
    void givenNoMatchingReference_whenValidatingEitherReference_thenErrorIsReported() {
      JsonNode payload =
        body("""
          {"carrierBookingRequestReference":"A","carrierBookingReference":"B"}
          """);

      assertErrorReported(
        BookingChecks.cbrrOrCbr(DRY_CARGO_PARAMETERS),
        payload,
        "Either 'carrierBookingRequestReference' must equal CBRR123 or 'carrierBookingReference' must equal CBR456.");
    }

    @Test
    void givenNoExpectedBookingReference_whenValidatingBookingReference_thenCheckIsIrrelevant() {
      Supplier<BookingDynamicScenarioParameters> parameters =
        () -> new BookingDynamicScenarioParameters(ScenarioType.DRY_CARGO.name(), CBRR, null);

      assertIrrelevant(BookingChecks.cbrValidation(parameters), emptyBody());
    }

    @Test
    void givenMismatchedBookingReference_whenValidatingBookingReference_thenErrorIsReported() {
      JsonNode payload = body("""
        {"carrierBookingReference":"WRONG"}
        """);

      assertErrorReported(
        BookingChecks.cbrValidation(DRY_CARGO_PARAMETERS),
        payload,
        "'carrierBookingReference' must equal CBR456.");
    }

    @Test
    void givenMatchingBookingReference_whenValidatingBookingReference_thenCheckPasses() {
      JsonNode payload = body("""
        {"carrierBookingReference":"CBR456"}
        """);

      assertConformant(BookingChecks.cbrValidation(DRY_CARGO_PARAMETERS), payload);
    }

    @Test
    void givenNoExpectedRequestReference_whenValidatingRequestReference_thenCheckIsIrrelevant() {
      Supplier<BookingDynamicScenarioParameters> parameters =
        () -> new BookingDynamicScenarioParameters(ScenarioType.DRY_CARGO.name(), null, CBR);

      assertIrrelevant(BookingChecks.cbrrValidation(parameters), emptyBody());
    }

    @Test
    void givenMismatchedRequestReference_whenValidatingRequestReference_thenErrorIsReported() {
      JsonNode payload = body("""
        {"carrierBookingRequestReference":"WRONG"}
        """);

      assertErrorReported(
        BookingChecks.cbrrValidation(DRY_CARGO_PARAMETERS),
        payload,
        "'carrierBookingRequestReference' must equal CBRR123.");
    }

    @Test
    void givenMatchingRequestReference_whenValidatingRequestReference_thenCheckPasses() {
      JsonNode payload = body("""
        {"carrierBookingRequestReference":"CBRR123"}
        """);

      assertConformant(BookingChecks.cbrrValidation(DRY_CARGO_PARAMETERS), payload);
    }
  }
}
