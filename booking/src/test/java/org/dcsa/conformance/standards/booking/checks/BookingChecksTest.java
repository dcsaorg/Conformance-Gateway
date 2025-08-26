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
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;
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
}
