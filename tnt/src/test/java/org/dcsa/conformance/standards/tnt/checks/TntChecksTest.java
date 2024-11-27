package org.dcsa.conformance.standards.tnt.checks;

import static org.dcsa.conformance.standards.tnt.action.TntEventType.*;
import static org.dcsa.conformance.standards.tnt.party.TntFilterParameter.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.tnt.action.TntEventType;
import org.dcsa.conformance.standards.tnt.party.TntFilterParameter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TntChecksTest {
  private static final OffsetDateTime TODAY = OffsetDateTime.now();
  private static final OffsetDateTime TOMORROW = TODAY.plusDays(1);

  private static Stream<Arguments> provideEventCreatedDateTimeData() {
    return Stream.of(
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME_LT, TODAY, false),
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME_LTE, TODAY, true),
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME, TODAY, true),
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME_EQ, TODAY, true),
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME_GTE, TODAY, true),
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME_GT, TODAY, false),
        //
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME_LT, TOMORROW, true),
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME_LTE, TOMORROW, true),
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME, TOMORROW, false),
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME_EQ, TOMORROW, false),
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME_GTE, TOMORROW, false),
        Arguments.of(TODAY, EVENT_CREATED_DATE_TIME_GT, TOMORROW, false),
        //
        Arguments.of(TOMORROW, EVENT_CREATED_DATE_TIME_LT, TODAY, false),
        Arguments.of(TOMORROW, EVENT_CREATED_DATE_TIME_LTE, TODAY, false),
        Arguments.of(TOMORROW, EVENT_CREATED_DATE_TIME, TODAY, false),
        Arguments.of(TOMORROW, EVENT_CREATED_DATE_TIME_EQ, TODAY, false),
        Arguments.of(TOMORROW, EVENT_CREATED_DATE_TIME_GTE, TODAY, true),
        Arguments.of(TOMORROW, EVENT_CREATED_DATE_TIME_GT, TODAY, true));
  }

  @ParameterizedTest
  @MethodSource("provideEventCreatedDateTimeData")
  void testValidateEventCreatedDateTime(
      OffsetDateTime left,
      TntFilterParameter operator,
      OffsetDateTime right,
      boolean expectedResult) {
    String validationError = TntChecks.validateEventCreatedDateTime(left, operator, right);
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
        arrayOfNodes(
            documentReferenceNode("BKG", "bkg123"),
            documentReferenceNode("BKG", "bkg234"),
            documentReferenceNode("TRD", "trd123"),
            documentReferenceNode("TRD", "trd234"));
    Stream.of(TRANSPORT, EQUIPMENT)
        .forEach(
            eventType -> {
              ObjectNode eventWithCBR =
                  withChildNode(
                      eventJsonObject(eventType), "documentReferences", documentReferencesArray);

              assertTrue(
                  TntChecks.checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          filterParameters(
                              eventType, Map.entry(CARRIER_BOOKING_REFERENCE, "bkg234")))
                      .isEmpty());
              assertFalse(
                  TntChecks.checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          filterParameters(
                              eventType, Map.entry(CARRIER_BOOKING_REFERENCE, "wrong")))
                      .isEmpty());
              assertFalse(
                  TntChecks.checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          filterParameters(
                              eventType, Map.entry(CARRIER_BOOKING_REFERENCE, "trd123")))
                      .isEmpty());

              assertTrue(
                  TntChecks.checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          filterParameters(
                              eventType, Map.entry(TRANSPORT_DOCUMENT_REFERENCE, "trd123")))
                      .isEmpty());
              assertFalse(
                  TntChecks.checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          filterParameters(
                              eventType, Map.entry(TRANSPORT_DOCUMENT_REFERENCE, "wrong")))
                      .isEmpty());
              assertFalse(
                  TntChecks.checkThatEventValuesMatchParamValues(
                          eventWithCBR,
                          filterParameters(
                              eventType, Map.entry(TRANSPORT_DOCUMENT_REFERENCE, "bkg123")))
                      .isEmpty());
            });

    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(SHIPMENT)
                    .put("documentID", "bkg123")
                    .put("documentTypeCode", "BKG"),
                filterParameters(SHIPMENT, Map.entry(CARRIER_BOOKING_REFERENCE, "bkg123")))
            .isEmpty());
    assertFalse(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(SHIPMENT)
                    .put("documentID", "bkg234")
                    .put("documentTypeCode", "BKG"),
                filterParameters(SHIPMENT, Map.entry(CARRIER_BOOKING_REFERENCE, "bkg123")))
            .isEmpty());
    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(SHIPMENT)
                    .put("documentID", "bkg234")
                    .put(
                        "documentTypeCode", "CBR"), // which means something else in this context :|
                filterParameters(SHIPMENT, Map.entry(CARRIER_BOOKING_REFERENCE, "bkg123")))
            .isEmpty());

    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(SHIPMENT)
                    .put("documentID", "trd123")
                    .put("documentTypeCode", "TRD"),
                filterParameters(SHIPMENT, Map.entry(TRANSPORT_DOCUMENT_REFERENCE, "trd123")))
            .isEmpty());
    assertFalse(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(SHIPMENT)
                    .put("documentID", "trd123")
                    .put("documentTypeCode", "TRD"),
                filterParameters(SHIPMENT, Map.entry(TRANSPORT_DOCUMENT_REFERENCE, "wrong")))
            .isEmpty());
  }

  @Test
  void testMatchingDocumentTypeCode() {
    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(SHIPMENT).put("documentTypeCode", "CBR"),
                filterParameters(SHIPMENT, Map.entry(DOCUMENT_TYPE_CODE, "CBR")))
            .isEmpty());
    assertFalse(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(SHIPMENT).put("documentTypeCode", "CBR"),
                filterParameters(SHIPMENT, Map.entry(DOCUMENT_TYPE_CODE, "BKG")))
            .isEmpty());
    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(SHIPMENT).put("documentTypeCode", "CBR"),
                filterParameters(SHIPMENT, Map.entry(DOCUMENT_TYPE_CODE, "CBR,BKG")))
            .isEmpty());
  }

  @Test
  void testMatchingEventTypeCodes() {
    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(TRANSPORT).put("transportEventTypeCode", "ARRI"),
                filterParameters(TRANSPORT, Map.entry(TRANSPORT_EVENT_TYPE_CODE, "ARRI")))
            .isEmpty());
    assertFalse(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(TRANSPORT).put("transportEventTypeCode", "ARRI"),
                filterParameters(TRANSPORT, Map.entry(TRANSPORT_EVENT_TYPE_CODE, "DEPA")))
            .isEmpty());

    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(SHIPMENT).put("shipmentEventTypeCode", "RECE"),
                filterParameters(SHIPMENT, Map.entry(SHIPMENT_EVENT_TYPE_CODE, "RECE")))
            .isEmpty());
    assertFalse(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(SHIPMENT).put("shipmentEventTypeCode", "RECE"),
                filterParameters(SHIPMENT, Map.entry(SHIPMENT_EVENT_TYPE_CODE, "DRFT")))
            .isEmpty());

    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(EQUIPMENT).put("equipmentEventTypeCode", "LOAD"),
                filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_EVENT_TYPE_CODE, "LOAD")))
            .isEmpty());
    assertFalse(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventJsonObject(EQUIPMENT).put("equipmentEventTypeCode", "LOAD"),
                filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_EVENT_TYPE_CODE, "DISC")))
            .isEmpty());
  }

  @Test
  void testMatchingVesselImoNumber() {
    Stream.of(TRANSPORT, EQUIPMENT)
        .forEach(
            eventType -> {
              ObjectNode eventWithVesselImo =
                  withChildNode(
                      eventJsonObject(eventType),
                      "transportCall",
                      withChildNode(
                          JsonToolkit.OBJECT_MAPPER.createObjectNode(),
                          "vessel",
                          JsonToolkit.OBJECT_MAPPER
                              .createObjectNode()
                              .put("vesselIMONumber", "1234567")));

              assertTrue(
                  TntChecks.checkThatEventValuesMatchParamValues(
                          eventWithVesselImo,
                          filterParameters(eventType, Map.entry(VESSEL_IMO_NUMBER, "1234567")))
                      .isEmpty());
              assertFalse(
                  TntChecks.checkThatEventValuesMatchParamValues(
                          eventWithVesselImo,
                          filterParameters(eventType, Map.entry(VESSEL_IMO_NUMBER, "2345678")))
                      .isEmpty());
            });
  }

  @Test
  void testMatchingEquipmentReferences() {
    ObjectNode equipmentEvent =
        withChildNode(
            eventJsonObject(EQUIPMENT).put("equipmentReference", "ASDF1234567"),
            "references",
            arrayOfNodes(referenceNode("EQ", "ASDF2345678"), referenceNode("FF", "ff123")));
    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                equipmentEvent,
                filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_REFERENCE, "ASDF1234567")))
            .isEmpty());
    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                equipmentEvent,
                filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_REFERENCE, "ASDF2345678")))
            .isEmpty());
    assertFalse(
        TntChecks.checkThatEventValuesMatchParamValues(
                equipmentEvent,
                filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_REFERENCE, "ff123")))
            .isEmpty());
    assertFalse(
        TntChecks.checkThatEventValuesMatchParamValues(
                equipmentEvent,
                filterParameters(EQUIPMENT, Map.entry(EQUIPMENT_REFERENCE, "wrong")))
            .isEmpty());
  }

  @Test
  void testMatchingUnLocationCode() {
    ObjectNode eventWithVesselImo =
        withChildNode(
            withChildNode(
                eventJsonObject(EQUIPMENT),
                "eventLocation",
                JsonToolkit.OBJECT_MAPPER.createObjectNode().put("UNLocationCode", "FRPAR")),
            "transportCall",
            JsonToolkit.OBJECT_MAPPER.createObjectNode().put("UNLocationCode", "DEHAM"));

    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventWithVesselImo,
                filterParameters(EQUIPMENT, Map.entry(UN_LOCATION_CODE, "FRPAR")))
            .isEmpty());
    assertTrue(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventWithVesselImo,
                filterParameters(EQUIPMENT, Map.entry(UN_LOCATION_CODE, "DEHAM")))
            .isEmpty());
    assertFalse(
        TntChecks.checkThatEventValuesMatchParamValues(
                eventWithVesselImo,
                filterParameters(EQUIPMENT, Map.entry(UN_LOCATION_CODE, "NLRTM")))
            .isEmpty());
  }

  private static ObjectNode withChildNode(
      ObjectNode parentNode, String childName, JsonNode childNode) {
    parentNode.set(childName, childNode);
    return parentNode;
  }

  private static ArrayNode arrayOfNodes(JsonNode... elementNodes) {
    ArrayNode arrayNode = JsonToolkit.OBJECT_MAPPER.createArrayNode();
    Arrays.stream(elementNodes).forEach(arrayNode::add);
    return arrayNode;
  }

  private static ObjectNode documentReferenceNode(String type, String value) {
    return JsonToolkit.OBJECT_MAPPER
        .createObjectNode()
        .put("documentReferenceType", type)
        .put("documentReferenceValue", value);
  }

  private static ObjectNode referenceNode(String type, String value) {
    return JsonToolkit.OBJECT_MAPPER
        .createObjectNode()
        .put("referenceType", type)
        .put("referenceValue", value);
  }

  private static ObjectNode eventJsonObject(TntEventType eventType) {
    return JsonToolkit.OBJECT_MAPPER
        .createObjectNode()
        .put(EVENT_TYPE.getQueryParamName(), eventType.name());
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  private static Map<TntFilterParameter, String> filterParameters(
      TntEventType eventType, Map.Entry<TntFilterParameter, String>... mapEntryStream) {
    return Stream.concat(
            Stream.of(Map.entry(EVENT_TYPE, eventType.name())), Arrays.stream(mapEntryStream))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
