package org.dcsa.conformance.standards.tnt.checks;

import static org.junit.jupiter.api.Assertions.*;
import static org.dcsa.conformance.standards.tnt.action.TntEventType.*;
import static org.dcsa.conformance.standards.tnt.party.TntFilterParameter.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.tnt.action.TntEventType;
import org.dcsa.conformance.standards.tnt.party.TntFilterParameter;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TntChecksTest {
  private static final OffsetDateTime TODAY = OffsetDateTime.now();
  private static final OffsetDateTime TOMORROW = TODAY.plusDays(1);

  @Test
  void testValidateEventCreatedDateTime() {
    _check(TODAY, EVENT_CREATED_DATE_TIME_LT, TODAY, false);
    _check(TODAY, EVENT_CREATED_DATE_TIME_LTE, TODAY, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME, TODAY, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME_EQ, TODAY, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME_GTE, TODAY, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME_GT, TODAY, false);

    _check(TODAY, EVENT_CREATED_DATE_TIME_LT, TOMORROW, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME_LTE, TOMORROW, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME, TOMORROW, false);
    _check(TODAY, EVENT_CREATED_DATE_TIME_EQ, TOMORROW, false);
    _check(TODAY, EVENT_CREATED_DATE_TIME_GTE, TOMORROW, false);
    _check(TODAY, EVENT_CREATED_DATE_TIME_GT, TOMORROW, false);

    _check(TOMORROW, EVENT_CREATED_DATE_TIME_LT, TODAY, false);
    _check(TOMORROW, EVENT_CREATED_DATE_TIME_LTE, TODAY, false);
    _check(TOMORROW, EVENT_CREATED_DATE_TIME, TODAY, false);
    _check(TOMORROW, EVENT_CREATED_DATE_TIME_EQ, TODAY, false);
    _check(TOMORROW, EVENT_CREATED_DATE_TIME_GTE, TODAY, true);
    _check(TOMORROW, EVENT_CREATED_DATE_TIME_GT, TODAY, true);
  }

  private void _check(
      OffsetDateTime left,
      TntFilterParameter operator,
      OffsetDateTime right,
      boolean expectedResult) {
    String validationError = TntChecks._validateEventCreatedDateTime(left, operator, right);
    if (expectedResult) {
      assertNull(
          validationError,
          "Expected %s %s %s to return no error but got %s"
              .formatted(left, operator.getQueryParamName(), right, validationError));
    } else {
      assertNotNull(
          validationError,
          "Expected %s %s %s to return an error but got none"
              .formatted(left, operator.getQueryParamName(), right));
    }
  }

  @Test
  void testMatchingCbrAndTrd() {
    ArrayNode documentReferencesArray =
        _arrayOfNodes(
            _documentReferenceNode("BKG", "bkg123"),
            _documentReferenceNode("BKG", "bkg234"),
            _documentReferenceNode("TRD", "trd123"),
            _documentReferenceNode("TRD", "trd234"));
    Stream.of(TRANSPORT, EQUIPMENT)
        .forEach(
            eventType -> {
              ObjectNode eventWithCBR =
                  _withChildNode(
                      _eventJsonObject(eventType), "documentReferences", documentReferencesArray);

              assertTrue(
                  TntChecks._checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          _filterParameters(
                              eventType, Map.entry(CARRIER_BOOKING_REFERENCE, "bkg234")))
                      .isEmpty());
              assertFalse(
                  TntChecks._checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          _filterParameters(
                              eventType, Map.entry(CARRIER_BOOKING_REFERENCE, "wrong")))
                      .isEmpty());
              assertFalse(
                  TntChecks._checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          _filterParameters(
                              eventType, Map.entry(CARRIER_BOOKING_REFERENCE, "trd123")))
                      .isEmpty());

              assertTrue(
                  TntChecks._checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          _filterParameters(
                              eventType, Map.entry(TRANSPORT_DOCUMENT_REFERENCE, "trd123")))
                      .isEmpty());
              assertFalse(
                  TntChecks._checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          _filterParameters(
                              eventType, Map.entry(TRANSPORT_DOCUMENT_REFERENCE, "wrong")))
                      .isEmpty());
              assertFalse(
                  TntChecks._checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          _filterParameters(
                              eventType, Map.entry(TRANSPORT_DOCUMENT_REFERENCE, "bkg123")))
                      .isEmpty());
            });

    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(SHIPMENT)
                    .put("documentID", "bkg123")
                    .put("documentTypeCode", "BKG"),
                _filterParameters(SHIPMENT, Map.entry(CARRIER_BOOKING_REFERENCE, "bkg123")))
            .isEmpty());
    assertFalse(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(SHIPMENT)
                    .put("documentID", "bkg234")
                    .put("documentTypeCode", "BKG"),
                _filterParameters(SHIPMENT, Map.entry(CARRIER_BOOKING_REFERENCE, "bkg123")))
            .isEmpty());
    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(SHIPMENT)
                    .put("documentID", "bkg234")
                    .put(
                        "documentTypeCode", "CBR"), // which means something else in this context :|
                _filterParameters(SHIPMENT, Map.entry(CARRIER_BOOKING_REFERENCE, "bkg123")))
            .isEmpty());

    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(SHIPMENT)
                    .put("documentID", "trd123")
                    .put("documentTypeCode", "TRD"),
                _filterParameters(SHIPMENT, Map.entry(TRANSPORT_DOCUMENT_REFERENCE, "trd123")))
            .isEmpty());
    assertFalse(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(SHIPMENT)
                    .put("documentID", "trd123")
                    .put("documentTypeCode", "TRD"),
                _filterParameters(SHIPMENT, Map.entry(TRANSPORT_DOCUMENT_REFERENCE, "wrong")))
            .isEmpty());
  }

  @Test
  void testMatchingDocumentTypeCode() {
    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(SHIPMENT).put("documentTypeCode", "CBR"),
                _filterParameters(SHIPMENT, Map.entry(DOCUMENT_TYPE_CODE, "CBR")))
            .isEmpty());
    assertFalse(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(SHIPMENT).put("documentTypeCode", "CBR"),
                _filterParameters(SHIPMENT, Map.entry(DOCUMENT_TYPE_CODE, "BKG")))
            .isEmpty());
    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(SHIPMENT).put("documentTypeCode", "CBR"),
                _filterParameters(SHIPMENT, Map.entry(DOCUMENT_TYPE_CODE, "CBR,BKG")))
            .isEmpty());
  }

  @Test
  void testMatchingEventTypeCodes() {
    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(TRANSPORT).put("transportEventTypeCode", "ARRI"),
                _filterParameters(TRANSPORT, Map.entry(TRANSPORT_EVENT_TYPE_CODE, "ARRI")))
            .isEmpty());
    assertFalse(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(TRANSPORT).put("transportEventTypeCode", "ARRI"),
                _filterParameters(TRANSPORT, Map.entry(TRANSPORT_EVENT_TYPE_CODE, "DEPA")))
            .isEmpty());

    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(SHIPMENT).put("shipmentEventTypeCode", "RECE"),
                _filterParameters(SHIPMENT, Map.entry(SHIPMENT_EVENT_TYPE_CODE, "RECE")))
            .isEmpty());
    assertFalse(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(SHIPMENT).put("shipmentEventTypeCode", "RECE"),
                _filterParameters(SHIPMENT, Map.entry(SHIPMENT_EVENT_TYPE_CODE, "DRFT")))
            .isEmpty());

    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(EQUIPMENT).put("equipmentEventTypeCode", "LOAD"),
                _filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_EVENT_TYPE_CODE, "LOAD")))
            .isEmpty());
    assertFalse(
        TntChecks._checkThatEventValuesMatchParamValues(
                _eventJsonObject(EQUIPMENT).put("equipmentEventTypeCode", "LOAD"),
                _filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_EVENT_TYPE_CODE, "DISC")))
            .isEmpty());
  }

  @Test
  void testMatchingVesselImoNumber() {
    Stream.of(TRANSPORT, EQUIPMENT)
        .forEach(
            eventType -> {
              ObjectNode eventWithVesselImo =
                  _withChildNode(
                      _eventJsonObject(eventType),
                      "transportCall",
                      _withChildNode(
                          JsonToolkit.OBJECT_MAPPER.createObjectNode(),
                          "vessel",
                          JsonToolkit.OBJECT_MAPPER
                              .createObjectNode()
                              .put("vesselIMONumber", "1234567")));

              assertTrue(
                  TntChecks._checkThatEventValuesMatchParamValues(
                          eventWithVesselImo,
                          _filterParameters(eventType, Map.entry(VESSEL_IMO_NUMBER, "1234567")))
                      .isEmpty());
              assertFalse(
                  TntChecks._checkThatEventValuesMatchParamValues(
                          eventWithVesselImo,
                          _filterParameters(eventType, Map.entry(VESSEL_IMO_NUMBER, "2345678")))
                      .isEmpty());
            });
  }

  @Test
  void testMatchingEquipmentReferences() {
    ObjectNode equipmentEvent =
        _withChildNode(
            _eventJsonObject(EQUIPMENT).put("equipmentReference", "ASDF1234567"),
            "references",
            _arrayOfNodes(_referenceNode("EQ", "ASDF2345678"), _referenceNode("FF", "ff123")));
    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                equipmentEvent,
                _filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_REFERENCE, "ASDF1234567")))
            .isEmpty());
    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                equipmentEvent,
                _filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_REFERENCE, "ASDF2345678")))
            .isEmpty());
    assertFalse(
        TntChecks._checkThatEventValuesMatchParamValues(
                equipmentEvent,
                _filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_REFERENCE, "ff123")))
            .isEmpty());
    assertFalse(
        TntChecks._checkThatEventValuesMatchParamValues(
                equipmentEvent,
                _filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_REFERENCE, "wrong")))
            .isEmpty());
  }

  @Test
  void testMatchingUnLocationCode() {
    ObjectNode eventWithVesselImo =
        _withChildNode(
            _withChildNode(
                _eventJsonObject(EQUIPMENT),
                "eventLocation",
                JsonToolkit.OBJECT_MAPPER.createObjectNode().put("UNLocationCode", "FRPAR")),
            "transportCall",
            JsonToolkit.OBJECT_MAPPER.createObjectNode().put("UNLocationCode", "DEHAM"));

    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                eventWithVesselImo,
                _filterParameters(EQUIPMENT, Map.entry(UN_LOCATION_CODE, "FRPAR")))
            .isEmpty());
    assertTrue(
        TntChecks._checkThatEventValuesMatchParamValues(
                eventWithVesselImo,
                _filterParameters(EQUIPMENT, Map.entry(UN_LOCATION_CODE, "DEHAM")))
            .isEmpty());
    assertFalse(
        TntChecks._checkThatEventValuesMatchParamValues(
                eventWithVesselImo,
                _filterParameters(EQUIPMENT, Map.entry(UN_LOCATION_CODE, "NLRTM")))
            .isEmpty());
  }

  private static ObjectNode _withChildNode(
      ObjectNode parentNode, String childName, JsonNode childNode) {
    parentNode.set(childName, childNode);
    return parentNode;
  }

  private static ArrayNode _arrayOfNodes(JsonNode... elementNodes) {
    ArrayNode arrayNode = JsonToolkit.OBJECT_MAPPER.createArrayNode();
    Arrays.stream(elementNodes).forEach(arrayNode::add);
    return arrayNode;
  }

  private static ObjectNode _documentReferenceNode(String type, String value) {
    return JsonToolkit.OBJECT_MAPPER
        .createObjectNode()
        .put("documentReferenceType", type)
        .put("documentReferenceValue", value);
  }

  private static ObjectNode _referenceNode(String type, String value) {
    return JsonToolkit.OBJECT_MAPPER
        .createObjectNode()
        .put("referenceType", type)
        .put("referenceValue", value);
  }

  private static ObjectNode _eventJsonObject(TntEventType eventType) {
    return JsonToolkit.OBJECT_MAPPER
        .createObjectNode()
        .put(EVENT_TYPE.getQueryParamName(), eventType.name());
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  private static Map<TntFilterParameter, String> _filterParameters(
      TntEventType eventType, Map.Entry<TntFilterParameter, String>... mapEntryStream) {
    return Stream.concat(
            Stream.of(Map.entry(EVENT_TYPE, eventType.name())), Arrays.stream(mapEntryStream))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
