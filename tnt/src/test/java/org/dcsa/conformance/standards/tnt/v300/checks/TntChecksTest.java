package org.dcsa.conformance.standards.tnt.v300.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceResult;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.tnt.v300.action.TntEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TntChecksTest {

  private static final String VERSION = "3.0.0";
  private static final String AT_LEAST_ONE_EVENT =
    "At least one event must be included in the tested message.";
  private static final String EVENT_ID =
    "Every event must demonstrate the correct use of the `eventID` attribute: it must be present and not empty or blank.";
  private static final String EVENT_DATE_TIME =
    "Every event must demonstrate the correct use of the `eventDateTime` attribute: it must be present and not empty or blank.";
  private static final String EVENT_UPDATED_DATE_TIME =
    "Every event must demonstrate the correct use of the `eventUpdatedDateTime` attribute: it must be present and not empty or blank.";
  private static final String EVENT_CLASSIFIER =
    "The `eventClassification.eventClassifier` attribute within every event must be one of `ACTUAL`, `ESTIMATED`, or `PLANNED`.";
  private static final String EVENT_SUBTYPE =
    "For every event, the event-type-specific subtype attribute applicable to its `eventClassification.eventType` must contain a value allowed by the corresponding OpenAPI schema property: `shipmentEventType`, `transportEventType`, `equipmentEventType`, `iotEventType`, or `reeferEventType`.";
  private static final String DOCUMENT_REFERENCE_TYPE =
    "The `shipmentDetails.documentReference.type` attribute within every Shipment event must contain a value allowed by `DocumentReference.type` in the T&T 3.0.0 OpenAPI specification.";
  private static final String DOCUMENT_REFERENCE =
    "The `shipmentDetails.documentReference.reference` attribute within every Shipment event must be present and not empty or blank.";
  private static final String EVENT_LOCATION =
    "Every Transport event must demonstrate the correct use of the `eventLocation` object: it must be present and not empty";
  private static final String TRANSPORT_CALL_REFERENCE =
    "The `transportDetails.transportCall.transportCallReference` attribute within every Transport event must be present and not empty or blank.";
  private static final String VESSEL_TRANSPORT =
    "When `transportDetails.transportCall.modeOfTransport` is `VESSEL` or `BARGE`, every applicable Transport event must demonstrate the correct use of `transportDetails.transportCall.vesselTransport`: it must be present and not empty";
  private static final String TRUCK_TRANSPORT =
    "When `transportDetails.transportCall.modeOfTransport` is `TRUCK`, every applicable Transport event must demonstrate the correct use of `transportDetails.transportCall.truckTransport`: it must be present and not empty";
  private static final String RAIL_TRANSPORT =
    "When `transportDetails.transportCall.modeOfTransport` is `RAIL`, every applicable Transport event must demonstrate the correct use of `transportDetails.transportCall.railTransport`: it must be present and not empty";
  private static final String EQUIPMENT_REFERENCE =
    "The `equipmentDetails.equipmentReference` attribute within every applicable event must be present and not empty or blank.";
  private static final String ISO_EQUIPMENT_CODE =
    "The `equipmentDetails.ISOEquipmentCode` attribute within every applicable event must be present and not empty or blank.";

  private static final List<String> DOCUMENTED_CHECKS = List.of(
    AT_LEAST_ONE_EVENT,
    EVENT_ID,
    EVENT_DATE_TIME,
    EVENT_UPDATED_DATE_TIME,
    EVENT_CLASSIFIER,
    EVENT_SUBTYPE,
    DOCUMENT_REFERENCE_TYPE,
    DOCUMENT_REFERENCE,
    EVENT_LOCATION,
    TRANSPORT_CALL_REFERENCE,
    VESSEL_TRANSPORT,
    TRUCK_TRANSPORT,
    RAIL_TRANSPORT,
    EQUIPMENT_REFERENCE,
    ISO_EQUIPMENT_CODE);

  @Test
  void getChecksExactlyMatchDocumentWhenThereIsNoScenarioTarget() {
    ActionCheck check = TntChecks.getTntGetResponseChecks(UUID.randomUUID(), VERSION, null);

    assertEquals(DOCUMENTED_CHECKS, check.subChecksStream().map(TntChecksTest::description).toList());
  }

  @ParameterizedTest
  @EnumSource(TntEventType.class)
  void postChecksAddOnlyTheDocumentedScenarioTargetCheck(TntEventType eventType) {
    ActionCheck check = TntChecks.getTntPostPayloadChecks(UUID.randomUUID(), VERSION, eventType);
    List<String> expected = new java.util.ArrayList<>(DOCUMENTED_CHECKS);
    expected.add(6, targetEventTypeDescription(eventType));

    assertEquals(expected, check.subChecksStream().map(TntChecksTest::description).toList());
  }

  @ParameterizedTest
  @EnumSource(TntEventType.class)
  void validEventPassesEveryApplicableCheck(TntEventType eventType) {
    Map<String, ConformanceResult> results = runPostChecks(body(validEvent(eventType)), eventType);

    assertTrue(results.values().stream().allMatch(ConformanceResult::isConformant));
    assertTrue(results.get(EVENT_SUBTYPE).isRelevant());
    assertTrue(results.get(targetEventTypeDescription(eventType)).isRelevant());
  }

  @ParameterizedTest
  @MethodSource("eventSubtypeCases")
  void subtypeValidationIsSeparateAndCoversEveryEventType(
    TntEventType eventType, String subtypeAttribute) {
    ObjectNode event = validEvent(eventType);
    event.withObject("eventClassification").put(subtypeAttribute, "NOT_A_VALID_SUBTYPE");

    assertFails(runPostChecks(body(event), eventType), EVENT_SUBTYPE);
  }

  @Test
  void subtypeValidationIsInapplicableToAnUnknownEventType() {
    ObjectNode event = commonEvent("UNKNOWN");

    Map<String, ConformanceResult> results = runGetChecks(body(event), null);

    assertTrue(results.get(EVENT_SUBTYPE).isConformant());
    assertFalse(results.get(EVENT_SUBTYPE).isRelevant());
  }

  @ParameterizedTest
  @MethodSource("commonRequiredAttributeCases")
  void commonRequiredAttributesAreValidatedIndividually(
    String description, Consumer<ObjectNode> invalidator) {
    ObjectNode event = validEvent(TntEventType.SHIPMENT);
    invalidator.accept(event);

    assertFails(runGetChecks(body(event), null), description);
  }

  @Test
  void emptyEventsArrayFailsTheAtLeastOneEventCheck() {
    assertFails(runGetChecks(body(), null), AT_LEAST_ONE_EVENT);
  }

  @Test
  void everyEventSemanticsRejectASecondInvalidEvent() {
    ObjectNode invalidSecondEvent = validEvent(TntEventType.IOT);
    invalidSecondEvent.remove("eventID");

    assertFails(
      runGetChecks(body(validEvent(TntEventType.SHIPMENT), invalidSecondEvent), null),
      EVENT_ID);
  }

  @Test
  void scenarioTargetRequiresAtLeastOneMatchingEvent() {
    Map<String, ConformanceResult> results =
      runPostChecks(body(validEvent(TntEventType.TRANSPORT)), TntEventType.SHIPMENT);

    assertFails(results, targetEventTypeDescription(TntEventType.SHIPMENT));
  }

  @ParameterizedTest
  @MethodSource("shipmentValidationCases")
  void shipmentValidationsAreIndependent(String description, Consumer<ObjectNode> invalidator) {
    ObjectNode event = validEvent(TntEventType.SHIPMENT);
    invalidator.accept(event);

    assertFails(runGetChecks(body(event), null), description);
  }

  @ParameterizedTest
  @MethodSource("transportBaseValidationCases")
  void transportBaseValidationsAreIndependent(String description, Consumer<ObjectNode> invalidator) {
    ObjectNode event = validEvent(TntEventType.TRANSPORT);
    invalidator.accept(event);

    assertFails(runGetChecks(body(event), null), description);
  }

  @ParameterizedTest
  @MethodSource("transportModeCases")
  void eachTransportObjectRuleFailsIndependentlyWhenItsObjectIsMissing(
    String modeOfTransport, String transportObject, String description) {
    ObjectNode event = transportEvent(modeOfTransport, transportObject);
    transportCall(event).remove(transportObject);

    Map<String, ConformanceResult> results = runGetChecks(body(event), null);

    assertFails(results, description);
    transportModeDescriptions().stream()
      .filter(otherDescription -> !otherDescription.equals(description))
      .forEach(otherDescription -> assertTrue(results.get(otherDescription).isConformant()));
  }

  @ParameterizedTest
  @MethodSource("transportModeCases")
  void eachTransportObjectRuleRejectsAnEmptyObject(
    String modeOfTransport, String transportObject, String description) {
    ObjectNode event = transportEvent(modeOfTransport, transportObject);
    transportCall(event).set(transportObject, JsonToolkit.OBJECT_MAPPER.createObjectNode());

    assertFails(runGetChecks(body(event), null), description);
  }

  @Test
  void transportObjectRulesAreConditionalOnModeOfTransport() {
    ObjectNode event = validEvent(TntEventType.TRANSPORT);
    ObjectNode transportCall = transportCall(event);
    transportCall.remove("modeOfTransport");
    transportCall.remove("vesselTransport");

    Map<String, ConformanceResult> results = runGetChecks(body(event), null);

    transportModeDescriptions().forEach(description -> {
      assertTrue(results.get(description).isConformant());
      assertFalse(results.get(description).isRelevant());
    });
  }

  @Test
  void eventSpecificChecksApplyEvenWhenTheScenarioTargetsAnotherEventType() {
    ObjectNode invalidTransportEvent = validEvent(TntEventType.TRANSPORT);
    invalidTransportEvent.set("eventLocation", JsonToolkit.OBJECT_MAPPER.createObjectNode());

    Map<String, ConformanceResult> results = runPostChecks(
      body(validEvent(TntEventType.SHIPMENT), invalidTransportEvent), TntEventType.SHIPMENT);

    assertFails(results, EVENT_LOCATION);
    assertTrue(results.get(targetEventTypeDescription(TntEventType.SHIPMENT)).isConformant());
  }

  @ParameterizedTest
  @MethodSource("equipmentDetailCases")
  void equipmentDetailRulesApplyToAllThreeDocumentedEventTypes(
    TntEventType eventType, String attribute, String description) {
    ObjectNode event = validEvent(eventType);
    event.withObject("equipmentDetails").put(attribute, "   ");

    assertFails(runGetChecks(body(event), null), description);
  }

  @Test
  void eventSpecificChecksAreInapplicableToOtherEventTypes() {
    Map<String, ConformanceResult> results =
      runGetChecks(body(validEvent(TntEventType.SHIPMENT)), null);

    Stream.of(EVENT_LOCATION, TRANSPORT_CALL_REFERENCE, VESSEL_TRANSPORT, TRUCK_TRANSPORT,
        RAIL_TRANSPORT, EQUIPMENT_REFERENCE, ISO_EQUIPMENT_CODE)
      .forEach(description -> {
        assertTrue(results.get(description).isConformant());
        assertFalse(results.get(description).isRelevant());
      });
  }

  private static Stream<Arguments> eventSubtypeCases() {
    return Stream.of(
      Arguments.of(TntEventType.SHIPMENT, "shipmentEventType"),
      Arguments.of(TntEventType.TRANSPORT, "transportEventType"),
      Arguments.of(TntEventType.EQUIPMENT, "equipmentEventType"),
      Arguments.of(TntEventType.IOT, "iotEventType"),
      Arguments.of(TntEventType.REEFER, "reeferEventType"));
  }

  private static Stream<Arguments> commonRequiredAttributeCases() {
    return Stream.of(
      Arguments.of(EVENT_ID, (Consumer<ObjectNode>) event -> event.remove("eventID")),
      Arguments.of(EVENT_DATE_TIME, (Consumer<ObjectNode>) event -> event.put("eventDateTime", "")),
      Arguments.of(EVENT_UPDATED_DATE_TIME,
        (Consumer<ObjectNode>) event -> event.put("eventUpdatedDateTime", "   ")),
      Arguments.of(EVENT_CLASSIFIER,
        (Consumer<ObjectNode>) event ->
          event.withObject("eventClassification").put("eventClassifier", "PREDICTED")));
  }

  private static Stream<Arguments> shipmentValidationCases() {
    return Stream.of(
      Arguments.of(DOCUMENT_REFERENCE_TYPE,
        (Consumer<ObjectNode>) event -> documentReference(event).put("type", "UNKNOWN")),
      Arguments.of(DOCUMENT_REFERENCE,
        (Consumer<ObjectNode>) event -> documentReference(event).put("reference", "   ")));
  }

  private static Stream<Arguments> transportBaseValidationCases() {
    return Stream.of(
      Arguments.of(EVENT_LOCATION,
        (Consumer<ObjectNode>) event ->
          event.set("eventLocation", JsonToolkit.OBJECT_MAPPER.createObjectNode())),
      Arguments.of(TRANSPORT_CALL_REFERENCE,
        (Consumer<ObjectNode>) event -> transportCall(event).put("transportCallReference", "")));
  }

  private static Stream<Arguments> transportModeCases() {
    return Stream.of(
      Arguments.of("VESSEL", "vesselTransport", VESSEL_TRANSPORT),
      Arguments.of("BARGE", "vesselTransport", VESSEL_TRANSPORT),
      Arguments.of("TRUCK", "truckTransport", TRUCK_TRANSPORT),
      Arguments.of("RAIL", "railTransport", RAIL_TRANSPORT));
  }

  private static Stream<Arguments> equipmentDetailCases() {
    return Stream.of(TntEventType.EQUIPMENT, TntEventType.IOT, TntEventType.REEFER)
      .flatMap(eventType -> Stream.of(
        Arguments.of(eventType, "equipmentReference", EQUIPMENT_REFERENCE),
        Arguments.of(eventType, "ISOEquipmentCode", ISO_EQUIPMENT_CODE)));
  }

  private static List<String> transportModeDescriptions() {
    return List.of(VESSEL_TRANSPORT, TRUCK_TRANSPORT, RAIL_TRANSPORT);
  }

  private static ObjectNode validEvent(TntEventType eventType) {
    return switch (eventType) {
      case SHIPMENT -> shipmentEvent();
      case TRANSPORT -> transportEvent("VESSEL", "vesselTransport");
      case EQUIPMENT -> equipmentRelatedEvent(eventType, "LOADED");
      case IOT -> equipmentRelatedEvent(eventType, "DOOR_OPENED");
      case REEFER -> equipmentRelatedEvent(eventType, "MEASURED");
    };
  }

  private static ObjectNode commonEvent(String eventType) {
    ObjectNode event = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    event.put("eventID", "event-1");
    event.put("eventDateTime", "2026-01-20T10:00:00Z");
    event.put("eventUpdatedDateTime", "2026-01-20T10:05:00Z");
    event.putObject("eventClassification")
      .put("eventType", eventType)
      .put("eventClassifier", "ACTUAL");
    return event;
  }

  private static ObjectNode shipmentEvent() {
    ObjectNode event = commonEvent(TntEventType.SHIPMENT.name());
    event.withObject("eventClassification").put("shipmentEventType", "CONFIRMED");
    event.putObject("shipmentDetails")
      .putObject("documentReference")
      .put("type", "BOOKING")
      .put("reference", "booking-reference");
    return event;
  }

  private static ObjectNode transportEvent(String modeOfTransport, String transportObject) {
    ObjectNode event = commonEvent(TntEventType.TRANSPORT.name());
    event.withObject("eventClassification").put("transportEventType", "ARRIVED");
    event.putObject("eventLocation").put("UNLocationCode", "NLRTM");
    ObjectNode transportCall = event.putObject("transportDetails").putObject("transportCall");
    transportCall.put("transportCallReference", "transport-call-1");
    transportCall.put("modeOfTransport", modeOfTransport);
    transportCall.putObject(transportObject).put("identifier", "present");
    return event;
  }

  private static ObjectNode equipmentRelatedEvent(TntEventType eventType, String subtype) {
    ObjectNode event = commonEvent(eventType.name());
    String subtypeAttribute = switch (eventType) {
      case EQUIPMENT -> "equipmentEventType";
      case IOT -> "iotEventType";
      case REEFER -> "reeferEventType";
      default -> throw new IllegalArgumentException("Not an equipment-related event type: " + eventType);
    };
    event.withObject("eventClassification").put(subtypeAttribute, subtype);
    event.putObject("equipmentDetails")
      .put("equipmentReference", "APZU4812090")
      .put("ISOEquipmentCode", "22GP");
    return event;
  }

  private static ObjectNode documentReference(ObjectNode event) {
    return event.withObject("shipmentDetails").withObject("documentReference");
  }

  private static ObjectNode transportCall(ObjectNode event) {
    return event.withObject("transportDetails").withObject("transportCall");
  }

  private static ObjectNode body(ObjectNode... events) {
    ObjectNode body = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    ArrayNode eventArray = body.putArray("events");
    Stream.of(events).forEach(eventArray::add);
    return body;
  }

  private static Map<String, ConformanceResult> runGetChecks(JsonNode body, TntEventType target) {
    UUID exchangeId = UUID.randomUUID();
    ActionCheck check = TntChecks.getTntGetResponseChecks(exchangeId, VERSION, target);
    return runChecks(check, exchange(body), exchangeId);
  }

  private static Map<String, ConformanceResult> runPostChecks(JsonNode body, TntEventType target) {
    UUID exchangeId = UUID.randomUUID();
    ActionCheck check = TntChecks.getTntPostPayloadChecks(exchangeId, VERSION, target);
    return runChecks(check, exchange(body), exchangeId);
  }

  private static Map<String, ConformanceResult> runChecks(
    ActionCheck check, ConformanceExchange exchange, UUID exchangeId) {
    check.check(requestedId -> exchangeId.equals(requestedId) ? exchange : null);
    return check.subChecksStream().collect(Collectors.toMap(
      TntChecksTest::description,
      subCheck -> subCheck.resultsStream().findFirst().orElseThrow(),
      (first, duplicate) -> {
        throw new AssertionError("Duplicate conformance check title: " + first);
      },
      LinkedHashMap::new));
  }

  private static String description(ConformanceCheck check) {
    return check.getTitle().stripLeading();
  }

  private static ConformanceExchange exchange(JsonNode body) {
    ConformanceMessage requestMessage = message(body);
    ConformanceMessage responseMessage = message(body);
    ConformanceRequest request =
      new ConformanceRequest("POST", "https://example.test/v3/events", Map.of(), requestMessage);
    return new ConformanceExchange(request, new ConformanceResponse(200, responseMessage));
  }

  private static ConformanceMessage message(JsonNode body) {
    return new ConformanceMessage(
      "source", "EventProvider", "target", "EventConsumer", Map.of(),
      new ConformanceMessageBody(body), System.currentTimeMillis());
  }

  private static String targetEventTypeDescription(TntEventType eventType) {
    return "For a scenario targeting `%s`, at least one event must have `eventClassification.eventType` equal to `%s`."
      .formatted(eventType, eventType);
  }

  private static void assertFails(Map<String, ConformanceResult> results, String description) {
    ConformanceResult result = results.get(description);
    assertFalse(result.isConformant(), "Expected check to fail: " + description);
    assertFalse(result.getErrors().isEmpty(), "Expected check errors: " + description);
  }
}


