package org.dcsa.conformance.standards.tnt.v300;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TntSchemaResourcesTest {

  private static final String SCHEMA_PATH = "/standards/tnt/schemas/TNT_v3.0.0.yaml";
  private static final String MESSAGE_PATH = "/standards/tnt/messages/";
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  @ParameterizedTest
  @MethodSource("messageFixtures")
  void messageFixtureMatchesTnt300Schema(String filename, String schemaName) throws Exception {
    JsonNode message = readMessage(filename);

    assertTrue(
      JsonSchemaValidator.getInstance(SCHEMA_PATH, schemaName).validate(message).isEmpty(),
      () -> "%s does not match %s".formatted(filename, schemaName));
    assertNull(message.findValue("emptyIndicatorCode"),
      () -> filename + " contains the legacy TNT 2.2 property emptyIndicatorCode");
    assertNull(message.findValue("movementType"),
      () -> filename + " contains the legacy TNT 2.2 property movementType");
  }

  @Test
  void schemaResourceContainsTheUpdatedTnt300Definitions() throws Exception {
    JsonNode schema = readSchema();

    assertPropertyExists(schema, "EquipmentDetails", "emptyIndicator");
    assertPropertyExists(schema, "EquipmentDetails", "transportPhase");
    assertPropertyExists(schema, "IotDetails", "detectionSource");
    assertPropertyExists(schema, "IotDetails", "geofenceDetails");
    assertPropertyExists(schema, "GeofenceDetails", "geofenceId");
    assertPropertyExists(schema, "GeofenceDetails", "geofenceCollectionCode");
    assertPropertyExists(schema, "GeofenceDetails", "geofenceCollectionVersion");
    assertPropertyExists(schema, "GeofenceDetails", "customerGeofenceCode");

    assertTrue(
      schema.path("components").path("schemas").path("EquipmentDetails").path("properties").path("emptyIndicatorCode").isMissingNode(),
      "EquipmentDetails should not define legacy property emptyIndicatorCode");
    assertTrue(
      schema.path("components").path("schemas").path("EquipmentDetails").path("properties").path("movementType").isMissingNode(),
      "EquipmentDetails should not define legacy property movementType");

    assertRemovedConstraintMissing(schema, "Address", "street", "maxLength");
    assertRemovedConstraintMissing(schema, "Address", "addressLines", "maxItems");
    assertRemovedConstraintMissing(schema, "EquipmentDetails", "equipmentReference", "maxLength");
    assertRemovedConstraintMissing(schema, "IotDetails", "deviceVendor", "maxLength");

    assertDescriptionContains(schema, "Address", "countryCode", "use the code `ZZ`");
    assertDescriptionContains(schema, "Event", "isRetracted", "isRetracted=false");
    assertDescriptionContains(schema, "Event", "isRetracted", "supersedes the retraction");
  }

  private static Stream<Arguments> messageFixtures() {
    return Stream.of(
      Arguments.of("tnt-300-request-shipment.json", "PostEventsRequest"),
      Arguments.of("tnt-300-request-transport.json", "PostEventsRequest"),
      Arguments.of("tnt-300-request-equipment.json", "PostEventsRequest"),
      Arguments.of("tnt-300-request-iot.json", "PostEventsRequest"),
      Arguments.of("tnt-300-request-reefer.json", "PostEventsRequest"),
      Arguments.of("tnt-300-response.json", "GetEventsResponse"),
      Arguments.of("tnt-300-response-nextpage.json", "GetEventsResponse"));
  }

  private static JsonNode readMessage(String filename) throws Exception {
    try (InputStream inputStream = TntSchemaResourcesTest.class.getResourceAsStream(MESSAGE_PATH + filename)) {
      assertNotNull(inputStream, () -> "Missing fixture: " + filename);
      return JsonToolkit.OBJECT_MAPPER.readTree(inputStream);
    }
  }

  private static JsonNode readSchema() throws Exception {
    try (InputStream inputStream = TntSchemaResourcesTest.class.getResourceAsStream(SCHEMA_PATH)) {
      assertNotNull(inputStream, "Missing schema resource: " + SCHEMA_PATH);
      return YAML_MAPPER.readTree(inputStream);
    }
  }

  private static void assertPropertyExists(JsonNode schema, String schemaName, String propertyName) {
    assertTrue(
      schema.path("components").path("schemas").path(schemaName).path("properties").has(propertyName),
      schemaName + " should define property " + propertyName);
  }

  private static void assertRemovedConstraintMissing(
    JsonNode schema, String schemaName, String propertyName, String constraintName) {
    assertTrue(
      schema.path("components").path("schemas").path(schemaName).path("properties").path(propertyName).path(constraintName).isMissingNode(),
      schemaName + "." + propertyName + " should not define constraint " + constraintName);
  }

  private static void assertDescriptionContains(
    JsonNode schema, String schemaName, String propertyName, String expectedText) {
    assertTrue(
      schema.path("components").path("schemas").path(schemaName).path("properties").path(propertyName).path("description").asText("").contains(expectedText),
      schemaName + "." + propertyName + " description should contain: " + expectedText);
  }
}


