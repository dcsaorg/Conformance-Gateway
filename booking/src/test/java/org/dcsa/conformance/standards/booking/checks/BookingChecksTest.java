package org.dcsa.conformance.standards.booking.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.booking.checks.BookingChecks.FEEDBACKS_PRESENCE;
import static org.dcsa.conformance.standards.booking.checks.BookingChecks.IS_EXPORT_DECLARATION_REFERENCE_PRESENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import java.util.function.Supplier;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingChecksTest {

  private ObjectNode booking;
  private ObjectNode requestedEquipment;
  private ArrayNode requestedEquipments;
  private ObjectNode commodity;
  private ArrayNode commodities;
  private Supplier<DynamicScenarioParameters> dspSupplier;
  private static final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    booking = OBJECT_MAPPER.createObjectNode();
    requestedEquipment = OBJECT_MAPPER.createObjectNode();
    requestedEquipments = OBJECT_MAPPER.createArrayNode();
    commodity = OBJECT_MAPPER.createObjectNode();
    commodities = OBJECT_MAPPER.createArrayNode();
    dspSupplier =
        () -> new DynamicScenarioParameters(ScenarioType.REGULAR, "CBRR123", "CBR456", null, null);
  }

  @Test
  void testCargoGrossWeightPresentAtRequestedEquipment_valid() {
    requestedEquipment.set("cargoGrossWeight", OBJECT_MAPPER.createObjectNode());
    requestedEquipments.add(requestedEquipment);
    booking.set("requestedEquipments", requestedEquipments);
    JsonContentCheck check = BookingChecks.CHECK_CARGO_GROSS_WEIGHT_CONDITIONS;
    Set<String> error = check.validate(booking);
    assertTrue(error.isEmpty());
  }

  @Test
  void testCargoGrossWeightMissingAtRequestedEquipmentNoCommodities_valid() {

    booking.set("requestedEquipments", requestedEquipments);
    JsonContentCheck check = BookingChecks.CHECK_CARGO_GROSS_WEIGHT_CONDITIONS;
    Set<String> errors = check.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCargoGrossWeightMissingAtRequestedEquipmentPresentAtCommodities_valid() {
    commodity.set("cargoGrossWeight", OBJECT_MAPPER.createObjectNode());
    commodities.add(commodity);
    requestedEquipment.set("commodities", commodities);
    requestedEquipments.add(requestedEquipment);
    booking.set("requestedEquipments", requestedEquipments);
    JsonContentCheck check = BookingChecks.CHECK_CARGO_GROSS_WEIGHT_CONDITIONS;
    Set<String> errors = check.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCargoGrossWeightMissingInRequestedEquipmentsAndAlsoInCommodities_invalid() {

    commodities.add(commodity);
    requestedEquipment.set("commodities", commodities);
    requestedEquipments.add(requestedEquipment);
    booking.set("requestedEquipments", requestedEquipments);

    JsonContentCheck check = BookingChecks.CHECK_CARGO_GROSS_WEIGHT_CONDITIONS;
    Set<String> errors = check.validate(booking);
    assertEquals(1, errors.size());
    assertTrue(
        errors.contains(
            "The 'requestedEquipments[0]' must have cargo gross weight at commodities position 0"));
  }

  @Test
  void testCargoGrossWeightMissingInRequestedEquipmentsAndInOneOfTheCommodities_invalid() {

    ObjectNode commodity2 = OBJECT_MAPPER.createObjectNode();
    commodity.set("cargoGrossWeight", OBJECT_MAPPER.createObjectNode());
    commodities.add(commodity2);
    commodities.add(commodity);
    requestedEquipment.set("commodities", commodities);
    requestedEquipments.add(requestedEquipment);
    booking.set("requestedEquipments", requestedEquipments);
    JsonContentCheck check = BookingChecks.CHECK_CARGO_GROSS_WEIGHT_CONDITIONS;
    Set<String> errors = check.validate(booking);
    assertEquals(1, errors.size());
    assertTrue(
        errors.contains(
            "The 'requestedEquipments[0]' must have cargo gross weight at commodities position 0"));
  }

  @Test
  void testIsExportDeclarationReferencePresence_requiredAndPresent() {
    booking.put("isExportDeclarationRequired", true);
    booking.put("exportDeclarationReference", "testReference");
    Set<String> errors = IS_EXPORT_DECLARATION_REFERENCE_PRESENCE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testIsExportDeclarationReferencePresence_requiredAndAbsent() {
    booking.put("isExportDeclarationRequired", true);
    Set<String> errors = IS_EXPORT_DECLARATION_REFERENCE_PRESENCE.validate(booking);
    assertEquals(1, errors.size());
  }

  @Test
  void testIsExportDeclarationReferencePresence_notRequiredAndAbsent() {
    booking.put("isExportDeclarationRequired", false);
    Set<String> errors = IS_EXPORT_DECLARATION_REFERENCE_PRESENCE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testIsExportDeclarationReferencePresence_notRequiredAndPresent() {
    booking.put("isExportDeclarationRequired", false);
    booking.put("exportDeclarationReference", "testReferenceValue");
    Set<String> errors = IS_EXPORT_DECLARATION_REFERENCE_PRESENCE.validate(booking);
    assertEquals(1, errors.size());
  }

  @Test
  void testIsExportDeclarationReferencePresence_missingFlagAndAbsent() {
    Set<String> errors = IS_EXPORT_DECLARATION_REFERENCE_PRESENCE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testIsExportDeclarationReferencePresence_missingFlagAndPresent() {
    booking.put("exportDeclarationReference", "testReference");
    Set<String> errors = IS_EXPORT_DECLARATION_REFERENCE_PRESENCE.validate(booking);
    assertEquals(1, errors.size());
  }

  @Test
  void testFeedbacksPresence() {
    booking.put("bookingStatus", BookingState.PENDING_UPDATE.name());
    assertFalse(FEEDBACKS_PRESENCE.validate(booking).isEmpty());

    booking.putArray("feedbacks").addObject().put("code", "INFORMATIONAL_MESSAGE");
    assertTrue(FEEDBACKS_PRESENCE.validate(booking).isEmpty());

    booking.put("bookingStatus", BookingState.PENDING_AMENDMENT.name());
    assertTrue(FEEDBACKS_PRESENCE.validate(booking).isEmpty());

    booking.remove("feedbacks");
    assertFalse(FEEDBACKS_PRESENCE.validate(booking).isEmpty());
  }

  @Test
  void bothReferencesCorrect() throws Exception {
    String json =
        """
            {
              "carrierBookingRequestReference": "CBRR123",
              "carrierBookingReference": "CBR456"
            }
            """;

    JsonNode body = mapper.readTree(json);
    Set<String> errors = BookingChecks.cbrrOrCbr(dspSupplier).validate(body);

    assertTrue(errors.isEmpty());
  }

  @Test
  void onlyCbrrCorrect() throws Exception {
    String json =
        """
            {
              "carrierBookingRequestReference": "CBRR123",
              "carrierBookingReference": "WRONG"
            }
            """;

    JsonNode body = mapper.readTree(json);
    Set<String> errors = BookingChecks.cbrrOrCbr(dspSupplier).validate(body);

    assertTrue(errors.isEmpty());
  }

  @Test
  void onlyCbrCorrect() throws Exception {
    String json =
        """
            {
              "carrierBookingRequestReference": "WRONG",
              "carrierBookingReference": "CBR456"
            }
            """;
    JsonNode body = mapper.readTree(json);
    Set<String> errors = BookingChecks.cbrrOrCbr(dspSupplier).validate(body);

    assertTrue(errors.isEmpty());
  }

  @Test
  void bothReferencesWrong() throws Exception {
    String json =
        """
            {
              "carrierBookingRequestReference": "WRONG1",
              "carrierBookingReference": "WRONG2"
            }
            """;

    JsonNode body = mapper.readTree(json);
    Set<String> errors = BookingChecks.cbrrOrCbr(dspSupplier).validate(body);

    assertFalse(errors.isEmpty());
    assertTrue(
        errors
            .iterator()
            .next()
            .contains(
                "Either 'carrierBookingRequestReference' must equal CBRR123 or 'carrierBookingReference' must equal CBR456"));
  }

  @Test
  void testDocumentPartyFunctionsUnique_noDocumentParties_valid() {
    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testDocumentPartyFunctionsUnique_noOtherParties_valid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testDocumentPartyFunctionsUnique_emptyOtherParties_valid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    ArrayNode otherParties = OBJECT_MAPPER.createArrayNode();
    documentParties.set("other", otherParties);
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testDocumentPartyFunctionsUnique_singlePartyFunction_valid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    ArrayNode otherParties = OBJECT_MAPPER.createArrayNode();
    ObjectNode party = OBJECT_MAPPER.createObjectNode();
    party.put("partyFunction", "DDR");
    otherParties.add(party);
    documentParties.set("other", otherParties);
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testDocumentPartyFunctionsUnique_uniquePartyFunctions_valid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    ArrayNode otherParties = OBJECT_MAPPER.createArrayNode();

    String[] partyFunctions = {"DDR", "DDS", "COW", "COX", "N1", "N2"};
    for (String function : partyFunctions) {
      ObjectNode party = OBJECT_MAPPER.createObjectNode();
      party.put("partyFunction", function);
      otherParties.add(party);
    }

    documentParties.set("other", otherParties);
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testDocumentPartyFunctionsUnique_multipleNI_valid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    ArrayNode otherParties = OBJECT_MAPPER.createArrayNode();

    for (int i = 0; i < 3; i++) {
      ObjectNode party = OBJECT_MAPPER.createObjectNode();
      party.put("partyFunction", "NI");
      otherParties.add(party);
    }

    documentParties.set("other", otherParties);
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testDocumentPartyFunctionsUnique_validExampleWithMultipleNI_valid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    ArrayNode otherParties = OBJECT_MAPPER.createArrayNode();

    String[] partyFunctions = {"DDR", "DDS", "COW", "COX", "N1", "N2", "NI", "NI", "NI"};
    for (String function : partyFunctions) {
      ObjectNode party = OBJECT_MAPPER.createObjectNode();
      party.put("partyFunction", function);
      otherParties.add(party);
    }

    documentParties.set("other", otherParties);
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testDocumentPartyFunctionsUnique_duplicateDDR_invalid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    ArrayNode otherParties = OBJECT_MAPPER.createArrayNode();

    ObjectNode party1 = OBJECT_MAPPER.createObjectNode();
    party1.put("partyFunction", "DDR");
    ObjectNode party2 = OBJECT_MAPPER.createObjectNode();
    party2.put("partyFunction", "DDR");

    otherParties.add(party1);
    otherParties.add(party2);
    documentParties.set("other", otherParties);
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertEquals(1, errors.size());
    assertTrue(errors.contains("Party function 'DDR' cannot be repeated. Found 2 occurrences."));
  }

  @Test
  void testDocumentPartyFunctionsUnique_multipleDuplicates_invalid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    ArrayNode otherParties = OBJECT_MAPPER.createArrayNode();

    String[] partyFunctions = {"DDR", "DDR", "DDS", "COW", "COW", "COW", "N1"};
    for (String function : partyFunctions) {
      ObjectNode party = OBJECT_MAPPER.createObjectNode();
      party.put("partyFunction", function);
      otherParties.add(party);
    }

    documentParties.set("other", otherParties);
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertEquals(2, errors.size());
    assertTrue(errors.contains("Party function 'DDR' cannot be repeated. Found 2 occurrences."));
    assertTrue(errors.contains("Party function 'COW' cannot be repeated. Found 3 occurrences."));
  }

  @Test
  void testDocumentPartyFunctionsUnique_duplicatesWithValidNI_invalid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    ArrayNode otherParties = OBJECT_MAPPER.createArrayNode();

    String[] partyFunctions = {"DDR", "DDS", "DDS", "NI", "NI", "NI", "COX", "COX"};
    for (String function : partyFunctions) {
      ObjectNode party = OBJECT_MAPPER.createObjectNode();
      party.put("partyFunction", function);
      otherParties.add(party);
    }

    documentParties.set("other", otherParties);
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertEquals(2, errors.size());
    assertTrue(errors.contains("Party function 'DDS' cannot be repeated. Found 2 occurrences."));
    assertTrue(errors.contains("Party function 'COX' cannot be repeated. Found 2 occurrences."));
  }

  @Test
  void testDocumentPartyFunctionsUnique_emptyPartyFunction_valid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    ArrayNode otherParties = OBJECT_MAPPER.createArrayNode();

    ObjectNode party1 = OBJECT_MAPPER.createObjectNode();
    party1.put("partyFunction", "");
    ObjectNode party2 = OBJECT_MAPPER.createObjectNode();
    party2.put("partyFunction", "DDR");
    ObjectNode party3 = OBJECT_MAPPER.createObjectNode();
    party3.put("partyFunction", "");

    otherParties.add(party1);
    otherParties.add(party2);
    otherParties.add(party3);
    documentParties.set("other", otherParties);
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testDocumentPartyFunctionsUnique_missingPartyFunction_valid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    ArrayNode otherParties = OBJECT_MAPPER.createArrayNode();

    ObjectNode party1 = OBJECT_MAPPER.createObjectNode();
    ObjectNode party2 = OBJECT_MAPPER.createObjectNode();
    party2.put("partyFunction", "DDR");

    otherParties.add(party1);
    otherParties.add(party2);
    documentParties.set("other", otherParties);
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testDocumentPartyFunctionsUnique_otherNotArray_valid() {
    ObjectNode documentParties = OBJECT_MAPPER.createObjectNode();
    documentParties.put("other", "not-an-array");
    booking.set("documentParties", documentParties);

    Set<String> errors = BookingChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCommoditiesSubreferenceUnique_noRequestedEquipments_valid() {
    // Test when requestedEquipments is missing entirely
    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCommoditiesSubreferenceUnique_emptyRequestedEquipments_valid() {
    // Test when requestedEquipments array is empty
    ArrayNode emptyEquipments = OBJECT_MAPPER.createArrayNode();
    booking.set("requestedEquipments", emptyEquipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCommoditiesSubreferenceUnique_noCommodities_valid() {
    // Test when equipment has no commodities
    ObjectNode equipment = OBJECT_MAPPER.createObjectNode();
    ArrayNode equipments = OBJECT_MAPPER.createArrayNode();
    equipments.add(equipment);
    booking.set("requestedEquipments", equipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCommoditiesSubreferenceUnique_emptyCommodities_valid() {
    // Test when commodities array is empty
    ObjectNode equipment = OBJECT_MAPPER.createObjectNode();
    ArrayNode commodities = OBJECT_MAPPER.createArrayNode();
    equipment.set("commodities", commodities);
    ArrayNode equipments = OBJECT_MAPPER.createArrayNode();
    equipments.add(equipment);
    booking.set("requestedEquipments", equipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCommoditiesSubreferenceUnique_singleCommodityWithSubref_valid() {
    // Test with single commodity having a subreference
    ObjectNode commodity = OBJECT_MAPPER.createObjectNode();
    commodity.put("commoditySubReference", "REF001");
    ArrayNode commodities = OBJECT_MAPPER.createArrayNode();
    commodities.add(commodity);

    ObjectNode equipment = OBJECT_MAPPER.createObjectNode();
    equipment.set("commodities", commodities);
    ArrayNode equipments = OBJECT_MAPPER.createArrayNode();
    equipments.add(equipment);
    booking.set("requestedEquipments", equipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCommoditiesSubreferenceUnique_multipleCommoditiesWithUniqueSubrefs_valid() {
    // Test with multiple commodities having unique subreferences within same equipment
    ObjectNode commodity1 = OBJECT_MAPPER.createObjectNode();
    commodity1.put("commoditySubReference", "REF001");
    ObjectNode commodity2 = OBJECT_MAPPER.createObjectNode();
    commodity2.put("commoditySubReference", "REF002");

    ArrayNode commodities = OBJECT_MAPPER.createArrayNode();
    commodities.add(commodity1);
    commodities.add(commodity2);

    ObjectNode equipment = OBJECT_MAPPER.createObjectNode();
    equipment.set("commodities", commodities);
    ArrayNode equipments = OBJECT_MAPPER.createArrayNode();
    equipments.add(equipment);
    booking.set("requestedEquipments", equipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCommoditiesSubreferenceUnique_multipleEquipmentsWithUniqueSubrefs_valid() {
    // Test with multiple equipment groups having unique subreferences across the entire booking
    // Equipment 1
    ObjectNode commodity1 = OBJECT_MAPPER.createObjectNode();
    commodity1.put("commoditySubReference", "REF001");
    ArrayNode commodities1 = OBJECT_MAPPER.createArrayNode();
    commodities1.add(commodity1);
    ObjectNode equipment1 = OBJECT_MAPPER.createObjectNode();
    equipment1.set("commodities", commodities1);

    // Equipment 2
    ObjectNode commodity2 = OBJECT_MAPPER.createObjectNode();
    commodity2.put("commoditySubReference", "REF002");
    ArrayNode commodities2 = OBJECT_MAPPER.createArrayNode();
    commodities2.add(commodity2);
    ObjectNode equipment2 = OBJECT_MAPPER.createObjectNode();
    equipment2.set("commodities", commodities2);

    ArrayNode equipments = OBJECT_MAPPER.createArrayNode();
    equipments.add(equipment1);
    equipments.add(equipment2);
    booking.set("requestedEquipments", equipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCommoditiesSubreferenceUnique_duplicateWithinSameEquipment_invalid() {
    // Test duplicate subreferences within the same equipment (should fail)
    ObjectNode commodity1 = OBJECT_MAPPER.createObjectNode();
    commodity1.put("commoditySubReference", "REF001");
    ObjectNode commodity2 = OBJECT_MAPPER.createObjectNode();
    commodity2.put("commoditySubReference", "REF001"); // Duplicate

    ArrayNode commodities = OBJECT_MAPPER.createArrayNode();
    commodities.add(commodity1);
    commodities.add(commodity2);

    ObjectNode equipment = OBJECT_MAPPER.createObjectNode();
    equipment.set("commodities", commodities);
    ArrayNode equipments = OBJECT_MAPPER.createArrayNode();
    equipments.add(equipment);
    booking.set("requestedEquipments", equipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertEquals(1, errors.size());
    assertTrue(
        errors.contains(
            "commoditySubReference 'REF001' is not unique across the booking. Found 2 occurrences."));
  }

  @Test
  void testCommoditiesSubreferenceUnique_duplicateAcrossEquipments_invalid() {
    // Test duplicate subreferences across different equipment groups (should fail)
    // Equipment 1
    ObjectNode commodity1 = OBJECT_MAPPER.createObjectNode();
    commodity1.put("commoditySubReference", "REF001");
    ArrayNode commodities1 = OBJECT_MAPPER.createArrayNode();
    commodities1.add(commodity1);
    ObjectNode equipment1 = OBJECT_MAPPER.createObjectNode();
    equipment1.set("commodities", commodities1);

    // Equipment 2
    ObjectNode commodity2 = OBJECT_MAPPER.createObjectNode();
    commodity2.put("commoditySubReference", "REF001"); // Duplicate across equipments
    ArrayNode commodities2 = OBJECT_MAPPER.createArrayNode();
    commodities2.add(commodity2);
    ObjectNode equipment2 = OBJECT_MAPPER.createObjectNode();
    equipment2.set("commodities", commodities2);

    ArrayNode equipments = OBJECT_MAPPER.createArrayNode();
    equipments.add(equipment1);
    equipments.add(equipment2);
    booking.set("requestedEquipments", equipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertEquals(1, errors.size());
    assertTrue(
        errors.contains(
            "commoditySubReference 'REF001' is not unique across the booking. Found 2 occurrences."));
  }

  @Test
  void testCommoditiesSubreferenceUnique_multipleDuplicates_invalid() {
    // Test multiple different duplicate subreferences
    ObjectNode commodity1 = OBJECT_MAPPER.createObjectNode();
    commodity1.put("commoditySubReference", "REF001");
    ObjectNode commodity2 = OBJECT_MAPPER.createObjectNode();
    commodity2.put("commoditySubReference", "REF001"); // Duplicate
    ObjectNode commodity3 = OBJECT_MAPPER.createObjectNode();
    commodity3.put("commoditySubReference", "REF002");
    ObjectNode commodity4 = OBJECT_MAPPER.createObjectNode();
    commodity4.put("commoditySubReference", "REF002"); // Another duplicate
    ObjectNode commodity5 = OBJECT_MAPPER.createObjectNode();
    commodity5.put("commoditySubReference", "REF002"); // Third occurrence

    ArrayNode commodities = OBJECT_MAPPER.createArrayNode();
    commodities.add(commodity1);
    commodities.add(commodity2);
    commodities.add(commodity3);
    commodities.add(commodity4);
    commodities.add(commodity5);

    ObjectNode equipment = OBJECT_MAPPER.createObjectNode();
    equipment.set("commodities", commodities);
    ArrayNode equipments = OBJECT_MAPPER.createArrayNode();
    equipments.add(equipment);
    booking.set("requestedEquipments", equipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertEquals(2, errors.size());
    assertTrue(
        errors.contains(
            "commoditySubReference 'REF001' is not unique across the booking. Found 2 occurrences."));
    assertTrue(
        errors.contains(
            "commoditySubReference 'REF002' is not unique across the booking. Found 3 occurrences."));
  }

  @Test
  void testCommoditiesSubreferenceUnique_emptySubreference_valid() {
    // Test with empty subreference values (should be ignored)
    ObjectNode commodity1 = OBJECT_MAPPER.createObjectNode();
    commodity1.put("commoditySubReference", ""); // Empty
    ObjectNode commodity2 = OBJECT_MAPPER.createObjectNode();
    commodity2.put("commoditySubReference", "REF001");
    ObjectNode commodity3 = OBJECT_MAPPER.createObjectNode();
    commodity3.put("commoditySubReference", ""); // Another empty

    ArrayNode commodities = OBJECT_MAPPER.createArrayNode();
    commodities.add(commodity1);
    commodities.add(commodity2);
    commodities.add(commodity3);

    ObjectNode equipment = OBJECT_MAPPER.createObjectNode();
    equipment.set("commodities", commodities);
    ArrayNode equipments = OBJECT_MAPPER.createArrayNode();
    equipments.add(equipment);
    booking.set("requestedEquipments", equipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCommoditiesSubreferenceUnique_missingSubreference_valid() {
    // Test with missing commoditySubReference fields (should be ignored)
    ObjectNode commodity1 = OBJECT_MAPPER.createObjectNode();
    // commodity1 has no commoditySubReference field
    ObjectNode commodity2 = OBJECT_MAPPER.createObjectNode();
    commodity2.put("commoditySubReference", "REF001");
    ObjectNode commodity3 = OBJECT_MAPPER.createObjectNode();
    // commodity3 also has no commoditySubReference field

    ArrayNode commodities = OBJECT_MAPPER.createArrayNode();
    commodities.add(commodity1);
    commodities.add(commodity2);
    commodities.add(commodity3);

    ObjectNode equipment = OBJECT_MAPPER.createObjectNode();
    equipment.set("commodities", commodities);
    ArrayNode equipments = OBJECT_MAPPER.createArrayNode();
    equipments.add(equipment);
    booking.set("requestedEquipments", equipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }

  @Test
  void testCommoditiesSubreferenceUnique_whitespaceOnlySubreference_valid() {
    // Test with whitespace-only subreference values (should be ignored due to isBlank() filter)
    ObjectNode commodity1 = OBJECT_MAPPER.createObjectNode();
    commodity1.put("commoditySubReference", "   "); // Whitespace only
    ObjectNode commodity2 = OBJECT_MAPPER.createObjectNode();
    commodity2.put("commoditySubReference", "REF001");
    ObjectNode commodity3 = OBJECT_MAPPER.createObjectNode();
    commodity3.put("commoditySubReference", "\t\n"); // More whitespace

    ArrayNode commodities = OBJECT_MAPPER.createArrayNode();
    commodities.add(commodity1);
    commodities.add(commodity2);
    commodities.add(commodity3);

    ObjectNode equipment = OBJECT_MAPPER.createObjectNode();
    equipment.set("commodities", commodities);
    ArrayNode equipments = OBJECT_MAPPER.createArrayNode();
    equipments.add(equipment);
    booking.set("requestedEquipments", equipments);

    Set<String> errors = BookingChecks.COMMODITIES_SUBREFERENCE_UNIQUE.validate(booking);
    assertTrue(errors.isEmpty());
  }
}
