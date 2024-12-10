package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookingChecksTest {

  @Test
  void testCheckCargoGrossWeightConditions_valid() throws Exception {

    // Test case 1: cargoWeight is present
    ObjectNode booking = OBJECT_MAPPER.createObjectNode();
    ObjectNode requestedEquipment = OBJECT_MAPPER.createObjectNode();
    requestedEquipment.set("cargoWeight", OBJECT_MAPPER.createObjectNode());
    ArrayNode requestedEquipments = OBJECT_MAPPER.createArrayNode();
    requestedEquipments.add(requestedEquipment);

    booking.set("requestedEquipments", requestedEquipments);
    JsonNode jsonNode = OBJECT_MAPPER.readTree(booking.toString());

    JsonContentCheck check = BookingChecks.CHECK_CARGO_GROSS_WEIGHT_CONDITIONS;
    Set<String> errors = check.validate(jsonNode);

    assertTrue(errors.isEmpty());

    //Test case 2: cargoWeight is missing but requested equipment has no commodities

    ObjectNode booking2 = OBJECT_MAPPER.createObjectNode();
    ObjectNode requestedEquipment2 = OBJECT_MAPPER.createObjectNode();
    ArrayNode requestedEquipments2 = OBJECT_MAPPER.createArrayNode();
    requestedEquipments2.add(requestedEquipment2);

    booking2.set("requestedEquipments", requestedEquipments2);

    Set<String> errors2 = check.validate(booking2);
    assertTrue(errors2.isEmpty());

    // Test case 3: cargoWeight is missing but commodities has cargoGrossWeight

    ObjectNode booking3 = OBJECT_MAPPER.createObjectNode();
    ObjectNode requestedEquipment3 = OBJECT_MAPPER.createObjectNode();
    ArrayNode requestedEquipments3 = OBJECT_MAPPER.createArrayNode();
    requestedEquipments3.add(requestedEquipment3);

    booking3.set("requestedEquipments", requestedEquipments3);
    ObjectNode commodity = OBJECT_MAPPER.createObjectNode();
    commodity.set("cargoGrossWeight", OBJECT_MAPPER.createObjectNode());
    ArrayNode commodities = OBJECT_MAPPER.createArrayNode();
    commodities.add(commodity);
    requestedEquipment3.set("commodities", commodities);

    Set<String> errors3 = check.validate(booking3);
    assertTrue(errors3.isEmpty());
  }

  @Test
  void testCheckCargoGrossWeightConditions_invalid() throws Exception {

    // Test case 1: cargoWeight is missing and commodities present but cargoGrossWeight is missing
    ObjectNode booking = OBJECT_MAPPER.createObjectNode();
    ObjectNode requestedEquipment = OBJECT_MAPPER.createObjectNode();
    ArrayNode requestedEquipments = OBJECT_MAPPER.createArrayNode();
    requestedEquipments.add(requestedEquipment);

    booking.set("requestedEquipments", requestedEquipments);
    ArrayNode commodities = OBJECT_MAPPER.createArrayNode();
    ObjectNode commodity = OBJECT_MAPPER.createObjectNode();

    commodities.add(commodity); // Adding commodity without cargoGrossWeight

    requestedEquipment.set("commodities", commodities);

    JsonContentCheck check = BookingChecks.CHECK_CARGO_GROSS_WEIGHT_CONDITIONS;
    Set<String> errors = check.validate(booking);
    assertEquals(1, errors.size());
    assertTrue(
        errors.contains(
            "The 'requestedEquipments[0]' must have cargo gross weight at commodities position 0"));

    // Test case 2 : cargoWeight is missing and commodities present but cargoGrossWeight is missing
    // for some commodities

    ObjectNode booking2 = OBJECT_MAPPER.createObjectNode();
    ObjectNode requestedEquipment2 = OBJECT_MAPPER.createObjectNode();
    ArrayNode requestedEquipments2 = OBJECT_MAPPER.createArrayNode();
    requestedEquipments2.add(requestedEquipment2);

    booking2.set("requestedEquipments", requestedEquipments2);
    ArrayNode commodities2 = OBJECT_MAPPER.createArrayNode();
    ObjectNode commodity2 = OBJECT_MAPPER.createObjectNode();
    ObjectNode commodity3 = OBJECT_MAPPER.createObjectNode();
    commodity3.set("cargoGrossWeight", OBJECT_MAPPER.createObjectNode());

    commodities2.add(commodity2); // Adding commodity without cargoGrossWeight
    commodities2.add(commodity3); // Adding commodity with cargoGrossWeight

    requestedEquipment2.set("commodities", commodities2);

    Set<String> errors2 = check.validate(booking2);
    assertEquals(1, errors2.size());
    assertTrue(
        errors2.contains(
            "The 'requestedEquipments[0]' must have cargo gross weight at commodities position 0"));
  }
}
