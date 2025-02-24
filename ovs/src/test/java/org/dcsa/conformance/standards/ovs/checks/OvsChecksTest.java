package org.dcsa.conformance.standards.ovs.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.ovs.party.OvsFilterParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OvsChecksTest {

  private JsonNode serviceNodes;

  @BeforeEach
  void setUp() {
    JsonNode vesselSchedules = createServiceVesselSchedules("1234567", "Great Vessel");
    serviceNodes = createServiceNodes("Great Lion Service", "GLS", "SR12345A", vesselSchedules);
  }

  @Test
  void testResponseContentChecks_validResponse() {
    Set<String> issues =
        executeResponseChecks(Map.of(OvsFilterParameter.CARRIER_SERVICE_CODE, "GLS"), serviceNodes);
    assertTrue(issues.isEmpty());
  }

  @Test
  void testResponseContentChecks_withWrongAttributesValuesResponse() {
    JsonNode jsonBody = JsonToolkit.templateFileToJsonNode(
      "/messages/ovs-300-response-wrong-attribute-values.json",
      Map.ofEntries());

    Set<String> issues =
      executeResponseChecks(Map.of(OvsFilterParameter.CARRIER_SERVICE_CODE, "GLS"), jsonBody);
    assertFalse(issues.isEmpty());
  }

  @Test
  void testResponseContentChecks_withWrongStructureResponse() {
    JsonNode jsonBody = JsonToolkit.templateFileToJsonNode(
      "/messages/ovs-300-response-wrong-structure.json",
      Map.ofEntries());

    Set<String> issues =
      executeResponseChecks(Map.of(OvsFilterParameter.CARRIER_SERVICE_CODE, ""), jsonBody);
    assertFalse(issues.isEmpty());
  }

  private Set<String> executeResponseChecks(
      Map<OvsFilterParameter, String> ovsFilterParameterStringMap, JsonNode serviceNodes) {
    Set<String> issues = new HashSet<>();

    OvsChecks.buildResponseContentChecks(ovsFilterParameterStringMap)
        .forEach(
            validator -> {
              issues.addAll(validator.validate(serviceNodes));
            });
    return issues;
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
  void testCheckCarrierServiceName_match() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.CARRIER_SERVICE_NAME, "Great Lion Service");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertTrue(result.isEmpty());
  }

  @Test
  void testCheckCarrierServiceName_noMatch() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.CARRIER_SERVICE_NAME, "Great Tiger Service");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertFalse(result.isEmpty());
  }

  @Test
  void testCheckUniversalServiceReference_match() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.UNIVERSAL_SERVICE_REFERENCE, "SR12345A");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertTrue(result.isEmpty());
  }

  @Test
  void testCheckUniversalServiceReference_noMatch() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.UNIVERSAL_SERVICE_REFERENCE, "SRA");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertFalse(result.isEmpty());
  }

  @Test
  void testCheckVesselIMONumber_match() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.VESSEL_IMO_NUMBER, "1234567");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertTrue(result.isEmpty());
  }

  @Test
  void testCheckVesselIMONumber_noMatch() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.VESSEL_IMO_NUMBER, "1234");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertFalse(result.isEmpty());
  }

  @Test
  void testCheckVesselName_match() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.VESSEL_NAME, "Great Vessel");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertTrue(result.isEmpty());
  }

  @Test
  void testCheckVesselName_noMatch() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.VESSEL_NAME, "Great Bowl");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertFalse(result.isEmpty());
  }

  @Test
  void testCheckCarrierVoyageNumber_match() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.CARRIER_VOYAGE_NUMBER, "2104N,2104S");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertTrue(result.isEmpty());
  }

  @Test
  void testCheckCarrierVoyageNumber_noMatch() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.CARRIER_VOYAGE_NUMBER, "2104P,2104Q");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertFalse(result.isEmpty());
  }

  @Test
  void testCheckUniversalVoyageReference_match() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.UNIVERSAL_VOYAGE_REFERENCE, "SR12345A,SR45678A");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertTrue(result.isEmpty());
  }

  @Test
  void testCheckUniversalVoyageReference_noMatch() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.UNIVERSAL_VOYAGE_REFERENCE, "SR1245A,SR458A");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertFalse(result.isEmpty());
  }

  @Test
  void testCheckUNLocationCode_match() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.UN_LOCATION_CODE, "NLAMS");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertTrue(result.isEmpty());
  }

  @Test
  void testCheckUNLocationCode_noMatch() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.UN_LOCATION_CODE, "USNYC");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertFalse(result.isEmpty());
  }

  @Test
  void testCheckFacilitySMDGCode_match() {
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.FACILITY_SMDG_CODE, "APM");
    JsonNode transportCall =
        serviceNodes.get(0).get("vesselSchedules").get(0).get("transportCalls").get(0);
    ((ObjectNode) transportCall.get("location")).put("facilitySMDGCode", "APM");
    Set<String> result =
        OvsChecks.checkThatScheduleValuesMatchParamValues(serviceNodes, filterParametersMap);
    assertTrue(result.isEmpty());
  }

  @Test
  void testCheckFacilitySMDGCode_noMatch() {
    JsonNode transportCall =
        serviceNodes.get(0).get("vesselSchedules").get(0).get("transportCalls").get(0);
    ((ObjectNode) transportCall.get("location")).put("facilitySMDGCode", "APM");
    Map<OvsFilterParameter, String> filterParametersMap =
        Map.of(OvsFilterParameter.FACILITY_SMDG_CODE, "APP");
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
      OBJECT_MAPPER.createObjectNode().put("transportCallReference", "TCREF1");
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
    JsonNode root = OBJECT_MAPPER.createObjectNode().put("value", "test");
    Stream<JsonNode> result = OvsChecks.findMatchingNodes(root, "/").map(Map.Entry::getValue);
    assertEquals(1, result.count());
  }

  @Test
  void testFindMatchingNodes_arrayMatch() throws IOException {
    JsonNode root = OBJECT_MAPPER.readTree("[{\"a\": 1}, {\"b\": 2}]");
    Stream<JsonNode> result = OvsChecks.findMatchingNodes(root, "*").map(Map.Entry::getValue);
    ;
    assertEquals(2, result.count());
  }

  @Test
  void testFindMatchingNodes_emptyArrayMatch() throws IOException {
    JsonNode root = OBJECT_MAPPER.readTree("[]");
    Stream<JsonNode> result = OvsChecks.findMatchingNodes(root, "*").map(Map.Entry::getValue);
    ;
    assertEquals(0, result.count());
  }

  @Test
  void testCheckServiceSchedulesExist_emptyServiceSchedules() {
    JsonNode body = OBJECT_MAPPER.createArrayNode();
    Set<String> result = OvsChecks.checkServiceSchedulesExist(body);
    assertEquals(1, result.size());
  }

  @Test
  void testCheckServiceSchedulesExist_nullServiceNode() {
    Set<String> result = OvsChecks.checkServiceSchedulesExist(null);
    assertEquals(1, result.size());
  }

  // Helper method to create a sample JsonNode for vessel schedules
  private JsonNode createServiceNodes(
      String carrierServiceName,
      String carrierServiceCode,
      String universalServiceReference,
      JsonNode vesselSchedules) {

    // Create the root ArrayNode
    ArrayNode rootArrayNode = OBJECT_MAPPER.createArrayNode();

    // Create the first ObjectNode
    ObjectNode firstObjectNode = OBJECT_MAPPER.createObjectNode();
    firstObjectNode.put("carrierServiceName", carrierServiceName);
    firstObjectNode.put("carrierServiceCode", carrierServiceCode);
    firstObjectNode.put("universalServiceReference", universalServiceReference);
    firstObjectNode.set("vesselSchedules", vesselSchedules);

    rootArrayNode.add(firstObjectNode);
    return rootArrayNode;
  }

  private JsonNode createServiceVesselSchedules(String vesselIMONumber, String vesselName) {
    ArrayNode vesselSchedulesArrayNode = OBJECT_MAPPER.createArrayNode();
    ObjectNode vesselSchedule = OBJECT_MAPPER.createObjectNode();
    vesselSchedule.put("vesselIMONumber", vesselIMONumber);
    vesselSchedule.put("vesselName", vesselName);
    vesselSchedule.set(
        "transportCalls",
        createTransportCalls("TCREF1", "2104N", "2104S", "SR12345A", "SR45678A", "NLAMS"));
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
    ArrayNode transportCallsArrayNode = OBJECT_MAPPER.createArrayNode();
    ObjectNode transportCall = OBJECT_MAPPER.createObjectNode();
    transportCall.put("transportCallReference", transportCallReference);
    transportCall.put("carrierImportVoyageNumber", carrierImportVoyageNumber);
    transportCall.put("carrierExportVoyageNumber", carrierExportVoyageNumber);
    transportCall.put("universalImportVoyageReference", universalImportVoyageReference);
    transportCall.put("universalExportVoyageReference", universalExportVoyageReference);

    // Create the location ObjectNode for the first transportCall
    ObjectNode location = OBJECT_MAPPER.createObjectNode();
    location.put("UNLocationCode", UNLocationCode);
    transportCall.set("location", location);
    transportCall.set("timestamps", createTimestamps());
    transportCallsArrayNode.add(transportCall);
    return transportCallsArrayNode;
  }

  private JsonNode createEventDateTime(String eventDateTime) {
    // Create a timestamp for timestamps ArrayNode
    ObjectNode timestamp = OBJECT_MAPPER.createObjectNode();
    timestamp.put("eventTypeCode", "ARRI");
    timestamp.put("eventClassifierCode", "PLN");
    timestamp.put("eventDateTime", eventDateTime);
    return timestamp;
  }

  private JsonNode createTimestamps() {
    // Create the timestamps ArrayNode
    ArrayNode timestampsArrayNode = OBJECT_MAPPER.createArrayNode();
    timestampsArrayNode.add(createEventDateTime("2024-07-21T10:00:00Z"));
    timestampsArrayNode.add(createEventDateTime("2024-07-22T10:00:00Z"));
    timestampsArrayNode.add(createEventDateTime("2024-07-23T10:00:00Z"));
    return timestampsArrayNode;
  }
}
