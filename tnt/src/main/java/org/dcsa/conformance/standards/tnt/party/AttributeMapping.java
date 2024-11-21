package org.dcsa.conformance.standards.tnt.party;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.BiPredicate;

@Getter
public class AttributeMapping {
  private final String jsonPath;
  private final BiPredicate<JsonNode, String> condition;
  private final Set<String> values;
  private static final String DOCUMENT_TYPE_CODE = "documentTypeCode";
  private static final String EVENT_CREATED_DATE_TIME = "eventCreatedDateTime";

  public AttributeMapping(String jsonPath, BiPredicate<JsonNode, String> condition, Set<String> values) {
    this.jsonPath = jsonPath;
    this.condition = condition;
    this.values = values;
  }

  public static Map<String, List<AttributeMapping>> initializeAttributeMappings() {
    Map<String, List<AttributeMapping>> attributeMappings = new HashMap<>();

    addShipmentEventTypeCodeMappings(attributeMappings);
    addDocumentTypeCodeMappings(attributeMappings);
    addEquipmentTypeCodeMappings(attributeMappings);
    addEventTypeMappings(attributeMappings);
    addTransportEventTypeCodeMappings(attributeMappings);
    addTransportCallIDMappings(attributeMappings);
    addVesselIMONumberMappings(attributeMappings);
    addExportVoyageNumberMappings(attributeMappings);
    addCarrierServiceCodeMappings(attributeMappings);
    addUNLocationCodeMappings(attributeMappings);
    addEventCreatedDateTimeMappings(attributeMappings);
    addCarrierBookingReferenceMappings(attributeMappings);
    addEquipmentReferenceMappings(attributeMappings);
    addTransportDocumentReferenceMappings(attributeMappings);
    return attributeMappings;
  }

  private static void addShipmentEventTypeCodeMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("shipmentEventTypeCode", List.of(
      new AttributeMapping("shipmentEventTypeCode", (node, value) -> !node.isMissingNode() && node.asText().equals(value),
        Set.of("RECE", "DRFT", "PENA", "PENU", "REJE", "APPR", "ISSU", "SURR", "SUBM", "VOID", "CONF", "REQS", "CMPL", "HOLD", "RELS"))));
  }

  private static void addDocumentTypeCodeMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put(DOCUMENT_TYPE_CODE, List.of(
      new AttributeMapping(DOCUMENT_TYPE_CODE, (node, value) -> !node.isMissingNode() && node.asText().equals(value),
        Set.of("CBR", "BKG", "SHI", "SRM", "TRD", "ARN", "VGM", "CAS", "CUS", "DGD", "OOG"))));
  }

  private static void addEquipmentTypeCodeMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("equipmentTypeCode", List.of(
      new AttributeMapping("equipmentEventTypeCode", (node, value) -> !node.isMissingNode() && node.asText().equals(value),
        Set.of("LOAD", "DISC", "GTIN", "GTOT", "STUF", "STRP", "PICK", "DROP", "INSP", "RSEA", "RMVD"))));
  }

  private static void addEventTypeMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("eventType", List.of(
      new AttributeMapping("eventType", (node, value) -> !node.isMissingNode() && node.asText().equals(value),
        Set.of("SHIPMENT", "TRANSPORT", "EQUIPMENT"))));
  }

  private static void addTransportEventTypeCodeMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("transportEventTypeCode", List.of(
      new AttributeMapping("transportEventTypeCode", (node, value) -> !node.isMissingNode() && node.asText().equals(value),
        Set.of())));
  }

  private static void addTransportCallIDMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("transportCallID", List.of(
      new AttributeMapping("transportCall/transportCallID", (node, value) -> !node.isMissingNode() && node.asText().equals(value),
        Set.of())));
  }

  private static void addVesselIMONumberMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("vesselIMONumber", List.of(
      new AttributeMapping("transportCall/vessel/vesselIMONumber", (node, value) -> !node.isMissingNode() && node.asText().equals(value),
        Set.of())));
  }

  private static void addExportVoyageNumberMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("exportVoyageNumber", List.of(
      new AttributeMapping("transportCall/exportVoyageNumber", (node, value) -> !node.isMissingNode() && node.asText().equals(value),
        Set.of())));
  }

  private static void addCarrierServiceCodeMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("carrierServiceCode", List.of(
      new AttributeMapping("transportCall/carrierServiceCode", (node, value) -> !node.isMissingNode() && node.asText().equals(value),
        Set.of())));
  }

  private static void addUNLocationCodeMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("UNLocationCode", List.of(
      new AttributeMapping("transportCall/location/UNLocationCode", (node, value) -> !node.isMissingNode() && node.asText().equals(value),
        Set.of())));
  }


  private static void addEventCreatedDateTimeMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put(EVENT_CREATED_DATE_TIME, List.of(
      new AttributeMapping(EVENT_CREATED_DATE_TIME, (node, value) -> !node.isMissingNode() && OffsetDateTime.parse(node.asText()).isEqual(OffsetDateTime.parse(value)),
        Set.of())));

    attributeMappings.put("eventCreatedDateTime:gte", List.of(
      new AttributeMapping(EVENT_CREATED_DATE_TIME, (node, value) -> !node.isMissingNode() && (OffsetDateTime.parse(node.asText()).isAfter(OffsetDateTime.parse(value)) || OffsetDateTime.parse(node.asText()).isEqual(OffsetDateTime.parse(value))),
        Set.of())));

    attributeMappings.put("eventCreatedDateTime:gt", List.of(
      new AttributeMapping(EVENT_CREATED_DATE_TIME, (node, value) -> !node.isMissingNode() && OffsetDateTime.parse(node.asText()).isAfter(OffsetDateTime.parse(value)),
        Set.of())));

    attributeMappings.put("eventCreatedDateTime:lt", List.of(
      new AttributeMapping(EVENT_CREATED_DATE_TIME, (node, value) -> !node.isMissingNode() && OffsetDateTime.parse(node.asText()).isBefore(OffsetDateTime.parse(value)),
        Set.of())));

    attributeMappings.put("eventCreatedDateTime:lte", List.of(
      new AttributeMapping(EVENT_CREATED_DATE_TIME, (node, value) -> !node.isMissingNode() && (OffsetDateTime.parse(node.asText()).isBefore(OffsetDateTime.parse(value)) || OffsetDateTime.parse(node.asText()).isEqual(OffsetDateTime.parse(value))),
        Set.of())));

    attributeMappings.put("eventCreatedDateTime:eq", List.of(
      new AttributeMapping(EVENT_CREATED_DATE_TIME, (node, value) -> !node.isMissingNode() && OffsetDateTime.parse(node.asText()).isEqual(OffsetDateTime.parse(value)),
        Set.of())));
  }

  private static void addCarrierBookingReferenceMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("carrierBookingReference", List.of(
      new AttributeMapping("documentReferences/*", (node, value) -> !node.isMissingNode()
        && node.path("documentReferenceType").asText().equals("BKG") && node.path("documentReferenceValue").asText().equals(value),
        Set.of()),
      new AttributeMapping("/", (node, value) -> !node.isMissingNode()
        && node.path(DOCUMENT_TYPE_CODE).asText().equals("BKG") && node.path("documentID").asText().equals(value),
        Set.of())));
  }

  private static void addEquipmentReferenceMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("equipmentReference", List.of(
      new AttributeMapping("equipmentReference", (node, value) -> !node.isMissingNode(),
        Set.of()),
      new AttributeMapping("references/*", (node, value) -> !node.isMissingNode()
        && node.path("referenceType").asText().equals("EQ") && node.path("referenceValue").asText().equals(value),
        Set.of())));
  }

  private static void addTransportDocumentReferenceMappings(Map<String, List<AttributeMapping>> attributeMappings) {
    attributeMappings.put("transportDocumentReference", List.of(
      new AttributeMapping("documentReferences/*", (node, value) -> !node.isMissingNode()
        && node.path("documentReferenceType").asText().equals("TRD") && node.path("documentReferenceValue").asText().equals(value),
        Set.of()),
      new AttributeMapping("/", (node, value) -> !node.isMissingNode()
        && node.path(DOCUMENT_TYPE_CODE).asText().equals("TRD") && node.path("documentID").asText().equals(value),
        Set.of())));
  }

}
