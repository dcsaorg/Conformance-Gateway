package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookingChecksTest {

  private ObjectNode booking;
  private ObjectNode requestedEquipment;
  private ArrayNode requestedEquipments;
  private ObjectNode commodity;
  private ArrayNode commodities;

  @BeforeEach
  void setUp() {
    booking = OBJECT_MAPPER.createObjectNode();
    requestedEquipment = OBJECT_MAPPER.createObjectNode();
    requestedEquipments = OBJECT_MAPPER.createArrayNode();
    commodity = OBJECT_MAPPER.createObjectNode();
    commodities = OBJECT_MAPPER.createArrayNode();
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
}
