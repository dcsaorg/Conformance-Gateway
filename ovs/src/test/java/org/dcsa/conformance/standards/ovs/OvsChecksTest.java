package org.dcsa.conformance.standards.ovs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.standards.ovs.checks.OvsChecks;
import org.dcsa.conformance.standards.ovs.party.OvsFilterParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class OvsChecksTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private JsonNode serviceNodes;

  @BeforeEach
  void setUp() {
    JsonNode vesselSchedules = createServiceVesselSchedules("1234567", "Great Vessel");
    serviceNodes = createServiceNodes("Great Lion Service", "GLS", "SR12345A", vesselSchedules);
  }

  @Test
  void testCheckThatScheduleValuesMatchParamValues_match() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(
            OvsFilterParameter.CARRIER_SERVICE_CODE,
            "GLS",
            OvsFilterParameter.CARRIER_SERVICE_NAME,
            "Great Lion Service",
            OvsFilterParameter.UNIVERSAL_SERVICE_REFERENCE,
            "SR12345A");

    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertTrue(result.isEmpty());
  }

  @Test
  void testCheckThatScheduleValuesMatchParamValues_noMatch() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.CARRIER_SERVICE_CODE, "BW1");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertFalse(result.isEmpty());
  }

  @Test
  void testCheckThatScheduleValuesMatchParamValues_emptyParams() {
    Map<OvsFilterParameter, String> filterParametersMap = Collections.emptyMap();
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertTrue(result.isEmpty());
  }

  @Test
  void testValidateDate_dateWithinRange() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.START_DATE, "2024-07-19");
    Set<String> result =
        OvsChecks.validateDate(
            serviceNodes, filterParametersMap, OvsFilterParameter.START_DATE, LocalDate::isBefore);
    assertTrue(result.isEmpty());
  }

  @Test
  void testValidateDate_dateOutsideRange() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.START_DATE, "2024-07-30");
    Set<String> result =
        OvsChecks.validateDate(
            serviceNodes, filterParametersMap, OvsFilterParameter.START_DATE, LocalDate::isBefore);
    assertFalse(result.isEmpty());
  }

  @Test
  void testValidateDate_noStartDate() {
    Map<OvsFilterParameter, String> filterParametersMap = Collections.emptyMap();
    Set<String> result =
        OvsChecks.validateDate(
            serviceNodes, filterParametersMap, OvsFilterParameter.START_DATE, LocalDate::isBefore);
    assertTrue(result.isEmpty());
  }

  @Test
  void testValidateUniqueTransportCallReference_unique() {
    JsonNode vesselSchedules = createServiceVesselSchedules("1234567", "Great Vessel");
    JsonNode serviceNodes =
        createServiceNodes("Great Lion Service", "GLS", "SR12345A", vesselSchedules);
    JsonNode transportCalls = vesselSchedules.get(0).get("transportCalls");

    ObjectNode newTransportCall =
        new ObjectMapper().createObjectNode().put("transportCallReference", "TCREF2");
    ((ArrayNode) transportCalls).add(newTransportCall);
    Set<String> result = OvsChecks.validateUniqueTransportCallReference(serviceNodes);
    assertTrue(result.isEmpty());
  }

  @Test
  void testValidateUniqueTransportCallReference_duplicate() {
    JsonNode vesselSchedules = createServiceVesselSchedules("1234567", "Great Vessel");

    JsonNode serviceNodes =
        createServiceNodes("Great Lion Service", "GLS", "SR12345A", vesselSchedules);
    JsonNode transportCalls = vesselSchedules.get(0).get("transportCalls");

    ObjectNode newTransportCall =
        new ObjectMapper().createObjectNode().put("transportCallReference", "TCREF1");
    ((ArrayNode) transportCalls).add(newTransportCall);

    Set<String> result = OvsChecks.validateUniqueTransportCallReference(serviceNodes);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
  }

  @Test
  void testValidateUniqueTransportCallReference_noVesselSchedules() {
    ((ObjectNode) serviceNodes.get(0)).remove("vesselSchedules");
    Set<String> result = OvsChecks.validateUniqueTransportCallReference(serviceNodes);
    assertTrue(result.isEmpty());
  }

  @Test
  void testFindMatchingNodes_rootMatch() {
    JsonNode root = objectMapper.createObjectNode().put("value", "test");
    Stream<JsonNode> result = OvsChecks.findMatchingNodes(root, "/");
    assertEquals(1, result.count());
  }

  @Test
  void testFindMatchingNodes_arrayMatch() throws IOException {
    JsonNode root = objectMapper.readTree("[{\"a\": 1}, {\"b\": 2}]");
    Stream<JsonNode> result = OvsChecks.findMatchingNodes(root, "*");
    assertEquals(2, result.count());
  }

  @Test
  void testFindMatchingNodes_emptyArrayMatch() throws IOException {
    JsonNode root = objectMapper.readTree("[]");
    Stream<JsonNode> result = OvsChecks.findMatchingNodes(root, "*");
    assertEquals(0, result.count());
  }

  @Test
  void testCheckServiceSchedulesExist_emptyServiceSchedules() {
    JsonNode body = objectMapper.createArrayNode();
    Set<String> result = OvsChecks.checkServiceSchedulesExist(body);
    assertEquals(1, result.size()); // Empty array should not return errors
  }

  @Test
  void testCheckServiceSchedulesExist_nullServiceNode() {
    Set<String> result = OvsChecks.checkServiceSchedulesExist(null);
    assertEquals(1, result.size()); // Empty array should not return errors
  }

  @Test
  void testValidateDate_invalidDateFormat() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.START_DATE, "2024-07-19");
    JsonNode timeStamps =
        serviceNodes
            .get(0)
            .get("vesselSchedules")
            .get(0)
            .get("transportCalls")
            .get(0)
            .get("timestamps");

    ObjectNode invalidTimeStamp =
        new ObjectMapper().createObjectNode().put("eventDateTime", "TCREF1");
    ((ArrayNode) timeStamps).add(invalidTimeStamp);

    Set<String> result =
        OvsChecks.validateDate(
            serviceNodes, filterParametersMap, OvsFilterParameter.START_DATE, LocalDate::isBefore);
    assertTrue(result.isEmpty()); // Should contain errors about invalid date format
  }

  // Helper method to create a sample JsonNode for vessel schedules
  private JsonNode createServiceNodes(
      String carrierServiceName,
      String carrierServiceCode,
      String universalServiceReference,
      JsonNode vesselSchedules) {
    ObjectMapper objectMapper = new ObjectMapper();

    // Create the root ArrayNode
    ArrayNode rootArrayNode = objectMapper.createArrayNode();

    // Create the first ObjectNode
    ObjectNode firstObjectNode = objectMapper.createObjectNode();
    firstObjectNode.put("carrierServiceName", carrierServiceName);
    firstObjectNode.put("carrierServiceCode", carrierServiceCode);
    firstObjectNode.put("universalServiceReference", universalServiceReference);
    firstObjectNode.set("vesselSchedules", vesselSchedules);

    rootArrayNode.add(firstObjectNode);
    return rootArrayNode;
  }

  private JsonNode createServiceVesselSchedules(String vesselIMONumber, String vesselName) {
    ArrayNode vesselSchedulesArrayNode = objectMapper.createArrayNode();
    ObjectNode vesselSchedule = objectMapper.createObjectNode();
    vesselSchedule.put("vesselIMONumber", vesselIMONumber);
    vesselSchedule.put("vesselName", vesselName);
    vesselSchedule.set(
        "transportCalls",
        createTransportCalls("TCREF1", "2104N", "2104S", "SR12345A", "SR12345A", "NLAMS"));
    vesselSchedulesArrayNode.add(vesselSchedule);
    return vesselSchedulesArrayNode;
  }

  private JsonNode createTransportCalls(
      String transportCallReference,
      String carrierImportVoyageNumber,
      String carrierExportVoyageNumber,
      String universalImportVoyageReference,
      String universalExportVoyageReference,
      String UNLocationCode) {
    // Create the transportCalls ArrayNode for the vesselSchedule
    ArrayNode transportCallsArrayNode = objectMapper.createArrayNode();
    ObjectNode transportCall = objectMapper.createObjectNode();
    transportCall.put("transportCallReference", transportCallReference);
    transportCall.put("carrierImportVoyageNumber", carrierImportVoyageNumber);
    transportCall.put("carrierExportVoyageNumber", carrierExportVoyageNumber);
    transportCall.put("universalImportVoyageReference", universalImportVoyageReference);
    transportCall.put("universalExportVoyageReference", universalExportVoyageReference);

    // Create the location ObjectNode for the first transportCall
    ObjectNode location = objectMapper.createObjectNode();
    location.put("UNLocationCode", UNLocationCode);
    transportCall.set("location", location);
    transportCall.set("timestamps", createTimestamps());
    transportCallsArrayNode.add(transportCall);
    return transportCallsArrayNode;
  }

  private JsonNode createEventDateTime(String eventDateTime) {
    // Create a timestamp for timestamps ArrayNode
    ObjectNode timestamp = objectMapper.createObjectNode();
    timestamp.put("eventTypeCode", "ARRI");
    timestamp.put("eventClassifierCode", "PLN");
    timestamp.put("eventDateTime", eventDateTime);
    return timestamp;
  }

  private JsonNode createTimestamps() {
    // Create the timestamps ArrayNode
    ArrayNode timestampsArrayNode = objectMapper.createArrayNode();
    timestampsArrayNode.add(createEventDateTime("2024-07-21T10:00:00Z"));
    timestampsArrayNode.add(createEventDateTime("2024-07-22T10:00:00Z"));
    timestampsArrayNode.add(createEventDateTime("2024-07-23T10:00:00Z"));
    return timestampsArrayNode;
  }
}
