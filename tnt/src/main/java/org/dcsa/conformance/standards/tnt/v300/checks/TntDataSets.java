package org.dcsa.conformance.standards.tnt.v300.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@UtilityClass
public class TntDataSets {

  private static final String TNT_V300_SCHEMA = "/standards/tnt/schemas/TNT_v3.0.0.yaml";
  private static final Pattern BACKTICKED_CODE_PATTERN = Pattern.compile("`([^`]+)`");
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
  private static final JsonNode OPENAPI_SCHEMA_ROOT = loadOpenApiSchemaRoot();

  private static final Set<String> DEFAULT_EVENT_CLASSIFIERS = Set.of("ACTUAL", "ESTIMATED", "PLANNED");

  private static final Set<String> DEFAULT_SHIPMENT_EVENT_TYPES = Set.of(
    "AMENDMENT_CANCELLED", "AMENDMENT_CONFIRMED", "AMENDMENT_DECLINED",
    "AMENDMENT_IN_PROGRESS", "AMENDMENT_RECEIVED", "APPROVED",
    "CANCELLATION_CONFIRMED", "CANCELLATION_DECLINED", "CANCELLATION_RECEIVED",
    "CANCELLED", "COMPLETED", "CONFIRMED", "DECLINED", "DRAFTED", "ISSUED",
    "ON_HOLD", "PENDING_AMENDMENT", "PENDING_APPROVAL",
    "PENDING_SURRENDER_FOR_AMENDMENT", "PENDING_SURRENDER_FOR_DELIVERY",
    "PENDING_UPDATE", "RECEIVED", "REJECTED", "RELEASED", "REQUESTED",
    "SUBMITTED", "SURRENDERED", "SURRENDERED_FOR_AMENDMENT",
    "SURRENDERED_FOR_DELIVERY", "UPDATE_CANCELLED", "UPDATE_CONFIRMED",
    "UPDATE_DECLINED", "UPDATE_IN_PROGRESS", "UPDATE_RECEIVED", "VOIDED");

  private static final Set<String> DEFAULT_DOCUMENT_REFERENCE_TYPES = Set.of(
    "ADVANCE_MANIFEST_FILING", "ARRIVAL_NOTICE", "BOOKING", "CARGO_SURVEY",
    "CARRIER_BOOKING_REQUEST", "CERTIFICATE_OF_ANALYSIS", "CERTIFICATE_OF_ORIGIN",
    "CONTAINER_RELEASE_ORDER", "CONTRACT_QUOTATION", "CUSTOMS_CLEARANCE",
    "DANGEROUS_GOODS_DECLARATION", "DELIVERY_INSTRUCTIONS", "DELIVERY_ORDER",
    "FUMIGATION_CERTIFICATE", "HEALTH_CERTIFICATE", "INSPECTION_CERTIFICATE",
    "INVOICE", "OUT_OF_GAUGE_DECLARATION", "PHYTOSANITARY_CERTIFICATE",
    "PROOF_OF_DELIVERY", "SHIPPING_INSTRUCTIONS", "TRANSPORT_DOCUMENT",
    "TRANSPORT_ORDER", "VERIFIED_GROSS_MASS", "VETERINARY_CERTIFICATE");

  private static final Set<String> DEFAULT_TRANSPORT_EVENT_TYPES = Set.of("APPROACHING", "ARRIVED", "DEPARTED");

  private static final Set<String> DEFAULT_EQUIPMENT_EVENT_TYPES = Set.of(
    "AVAILABLE_FOR_DROP_OFF", "AVAILABLE_FOR_PICKUP", "CROSSED", "CUSTOMS_RELEASED",
    "CUSTOMS_SELECTED_FOR_INSPECTION", "CUSTOMS_SELECTED_FOR_SCAN", "DISCHARGED",
    "DROPPED_OFF", "GATED_IN", "GATED_OUT", "INSPECTED", "LOADED", "PICKED_UP",
    "REMOVED", "RESEALED", "STRIPPED", "STUFFED");

  private static final Set<String> DEFAULT_IOT_EVENT_TYPES = Set.of(
    "BREADCRUMB", "DEVICE_STATUS", "DOOR_CLOSED", "DOOR_OPENED",
    "GEOFENCE_ENTRY", "GEOFENCE_EXIT", "MOTION_START", "MOTION_STOP",
    "PAIRING", "UNPAIRING");

  private static final Set<String> DEFAULT_REEFER_EVENT_TYPES = Set.of("ADJUSTED", "MEASURED");

  static final Set<String> VALID_EVENT_CLASSIFIERS =
    loadValuesFromDescription(
      OPENAPI_SCHEMA_ROOT.path("components").path("schemas").path("EventClassification")
        .path("properties").path("eventClassifier"),
      DEFAULT_EVENT_CLASSIFIERS,
      "EventClassification.eventClassifier");

  static final Set<String> VALID_SHIPMENT_EVENT_TYPES =
    loadValuesFromDescription(
      OPENAPI_SCHEMA_ROOT.path("components").path("schemas").path("EventClassification")
        .path("properties").path("shipmentEventType"),
      DEFAULT_SHIPMENT_EVENT_TYPES,
      "EventClassification.shipmentEventType");

  static final Set<String> VALID_DOCUMENT_REFERENCE_TYPES =
    loadValuesFromDescription(
      OPENAPI_SCHEMA_ROOT.path("components").path("schemas").path("DocumentReference")
        .path("properties").path("type"),
      DEFAULT_DOCUMENT_REFERENCE_TYPES,
      "DocumentReference.type");

  static final Set<String> VALID_TRANSPORT_EVENT_TYPES =
    loadValuesFromDescription(
      OPENAPI_SCHEMA_ROOT.path("components").path("schemas").path("EventClassification")
        .path("properties").path("transportEventType"),
      DEFAULT_TRANSPORT_EVENT_TYPES,
      "EventClassification.transportEventType");

  static final Set<String> VALID_EQUIPMENT_EVENT_TYPES =
    loadValuesFromDescription(
      OPENAPI_SCHEMA_ROOT.path("components").path("schemas").path("EventClassification")
        .path("properties").path("equipmentEventType"),
      DEFAULT_EQUIPMENT_EVENT_TYPES,
      "EventClassification.equipmentEventType");

  static final Set<String> VALID_IOT_EVENT_TYPES =
    loadValuesFromDescription(
      OPENAPI_SCHEMA_ROOT.path("components").path("schemas").path("EventClassification")
        .path("properties").path("iotEventType"),
      DEFAULT_IOT_EVENT_TYPES,
      "EventClassification.iotEventType");

  static final Set<String> VALID_REEFER_EVENT_TYPES =
    loadValuesFromDescription(
      OPENAPI_SCHEMA_ROOT.path("components").path("schemas").path("EventClassification")
        .path("properties").path("reeferEventType"),
      DEFAULT_REEFER_EVENT_TYPES,
      "EventClassification.reeferEventType");

  private static JsonNode loadOpenApiSchemaRoot() {
    try (InputStream inputStream = TntDataSets.class.getResourceAsStream(TNT_V300_SCHEMA)) {
      if (inputStream == null) {
        log.warn("Could not load TnT schema resource at {}. Falling back to built-in datasets.", TNT_V300_SCHEMA);
        return YAML_MAPPER.createObjectNode();
      }
      return YAML_MAPPER.readTree(inputStream);
    } catch (IOException e) {
      log.warn("Failed parsing TnT schema resource {}. Falling back to built-in datasets.", TNT_V300_SCHEMA, e);
      return YAML_MAPPER.createObjectNode();
    }
  }

  private static Set<String> loadValuesFromDescription(JsonNode schemaPropertyNode, Set<String> fallbackValues, String propertyPath) {
    String description = schemaPropertyNode.path("description").asText("");
    Set<String> extractedValues = extractBacktickedValues(description);
    if (extractedValues.isEmpty()) {
      log.warn("No enum-like values found in description for {}. Using fallback values.", propertyPath);
      return fallbackValues;
    }
    return Set.copyOf(extractedValues);
  }

  private static Set<String> extractBacktickedValues(String description) {
    LinkedHashSet<String> values = new LinkedHashSet<>();
    Matcher matcher = BACKTICKED_CODE_PATTERN.matcher(description);
    while (matcher.find()) {
      values.add(matcher.group(1));
    }
    return values;
  }
}
