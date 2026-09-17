package org.dcsa.conformance.standards.tnt.v300;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
  void updatedOptionalIotFieldsAreAcceptedByRuntimeSchemaValidation() throws Exception {
    ObjectNode response = (ObjectNode) readMessage("tnt-300-response.json");
    ObjectNode iotDetails = (ObjectNode) response.path("events").get(3).path("iotDetails");
    iotDetails.put("detectionSource", "PLATFORM");
    iotDetails.putObject("geofenceDetails")
      .put("geofenceId", "geofence-042")
      .put("geofenceCollectionCode", "acme-yard-geofences")
      .put("geofenceCollectionVersion", "2026-05-01")
      .put("customerGeofenceCode", "JAX-AREA1");

    assertTrue(
      JsonSchemaValidator.getInstance(SCHEMA_PATH, "GetEventsResponse").validate(response).isEmpty());
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
}


