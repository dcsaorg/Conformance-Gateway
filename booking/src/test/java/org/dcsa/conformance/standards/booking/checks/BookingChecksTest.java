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

  private static final String ERROR_MESSAGE_MISSING_AMENDED_BOOKING_STATUS =
      "The attribute 'amendedBookingStatus' should have been present but was absent";
  private static final String ERROR_MESSAGE_WRONG_AMENDED_BOOKING_STATUS =
      "The value of 'amendedBookingStatus' was '%s' instead of '%s'";
  private static final String ERROR_MESSAGE_BOOKING_STATUS_PRESENT =
      "The attribute 'bookingStatus' should have been absent but was present and had value '%s'";

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
  void testValidateBookingAmendmentCancellation_valid() {
    booking.put("amendedBookingStatus", BookingState.AMENDMENT_CANCELLED.name());

    JsonContentCheck check = BookingChecks.validateBookingAmendmentCancellation();
    Set<String> errors = check.validate(booking);

    assertTrue(errors.isEmpty());
  }

  @Test
  void testValidateAmendedBookingCancellation_missingBookingAmendmentStatus() {
    JsonContentCheck check = BookingChecks.validateBookingAmendmentCancellation();
    Set<String> errors = check.validate(booking);

    assertFalse(errors.isEmpty());
    assertEquals(2, errors.size());
    assertTrue(errors.contains(ERROR_MESSAGE_MISSING_AMENDED_BOOKING_STATUS));
    assertTrue(
        errors.contains(
            ERROR_MESSAGE_WRONG_AMENDED_BOOKING_STATUS.formatted(
                "(absent)", BookingState.AMENDMENT_CANCELLED.name())));
  }

  @Test
  void testValidateAmendedBookingCancellation_wrongBookingAmendmentStatus() {
    booking.put("amendedBookingStatus", BookingState.CONFIRMED.name());

    JsonContentCheck check = BookingChecks.validateBookingAmendmentCancellation();
    Set<String> errors = check.validate(booking);

    assertFalse(errors.isEmpty());
    assertEquals(1, errors.size());
    assertTrue(
        errors.contains(
            ERROR_MESSAGE_WRONG_AMENDED_BOOKING_STATUS.formatted(
                BookingState.CONFIRMED.name(), BookingState.AMENDMENT_CANCELLED.name())));
  }

  @Test
  void testValidateBookingCancellation_bookingAmendmentStatusPresent() {
    booking.put("amendedBookingStatus", BookingState.AMENDMENT_CANCELLED.name());
    booking.put("bookingStatus", BookingState.CONFIRMED.name());

    JsonContentCheck check = BookingChecks.validateBookingAmendmentCancellation();
    Set<String> errors = check.validate(booking);

    assertFalse(errors.isEmpty());
    assertEquals(1, errors.size());
    assertTrue(
        errors.contains(
            ERROR_MESSAGE_BOOKING_STATUS_PRESENT.formatted(BookingState.CONFIRMED.name())));
  }

  @Test
  void testValidateBookingCancellation_nullBookingAmendmentNode() {
    JsonContentCheck check = BookingChecks.validateBookingAmendmentCancellation();
    Set<String> errors = check.validate(OBJECT_MAPPER.nullNode());

    assertFalse(errors.isEmpty());
    assertEquals(2, errors.size());
    assertTrue(errors.contains(ERROR_MESSAGE_MISSING_AMENDED_BOOKING_STATUS));
    assertTrue(
        errors.contains(
            ERROR_MESSAGE_WRONG_AMENDED_BOOKING_STATUS.formatted(
                "(absent)", BookingState.AMENDMENT_CANCELLED.name())));
  }

  @Test
  void testValidateBookingCancellation_emptyBookingAmendmentNode() {
    JsonContentCheck check = BookingChecks.validateBookingAmendmentCancellation();
    Set<String> errors = check.validate(OBJECT_MAPPER.createObjectNode());

    assertFalse(errors.isEmpty());
    assertEquals(2, errors.size());
    assertTrue(errors.contains(ERROR_MESSAGE_MISSING_AMENDED_BOOKING_STATUS));
    assertTrue(
        errors.contains(
            ERROR_MESSAGE_WRONG_AMENDED_BOOKING_STATUS.formatted(
                "(absent)", BookingState.AMENDMENT_CANCELLED.name())));
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
}
