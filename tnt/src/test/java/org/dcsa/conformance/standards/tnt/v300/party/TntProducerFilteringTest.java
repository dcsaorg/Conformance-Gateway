package org.dcsa.conformance.standards.tnt.v300.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.state.MemorySortedPartitionsNonLockingMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.tnt.v300.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.tnt.v300.action.TntEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TntProducerFilteringTest {

  @Test
  void shouldFilterByEventTypesAndRespectLimit() {
    TntProducer producer = createProducer();

    ConformanceResponse response =
      producer.handleRequest(
        createGetEventsRequest(
          Map.of(
            TntQueryParameters.ET.getParameterName(), List.of("EQUIPMENT,IOT"),
            TntQueryParameters.LIMIT.getParameterName(), List.of("1"))));

    JsonNode events = response.message().body().getJsonBody().path(TntConstants.EVENTS);
    assertEquals(1, events.size());
    assertTrue(
      StreamSupport.stream(events.spliterator(), false)
        .allMatch(
          event -> {
            String eventType =
              event
                .path(TntConstants.EVENT_CLASSIFICATION)
                .path(TntConstants.EVENT_TYPE)
                .asText();
            return "EQUIPMENT".equals(eventType) || "IOT".equals(eventType);
          }));
    assertTrue(response.message().headers().containsKey(TntConstants.HEADER_CURSOR_NAME));
  }

  @Test
  void shouldFilterByMandatoryAndOptionalQueryParametersTogether() {
    TntProducer producer = createProducer();

    String cbr = "CBR-123";
    String from = "2025-01-24T00:00:00Z";
    String to = "2025-01-26T23:59:59Z";

    ConformanceResponse response =
      producer.handleRequest(
        createGetEventsRequest(
          Map.of(
            TntQueryParameters.ET.getParameterName(), List.of("TRANSPORT"),
            TntQueryParameters.CBR.getParameterName(), List.of(cbr),
            TntQueryParameters.E_UDT_MIN.getParameterName(), List.of(from),
            TntQueryParameters.E_UDT_MAX.getParameterName(), List.of(to))));

    JsonNode events = response.message().body().getJsonBody().path(TntConstants.EVENTS);
    assertEquals(1, events.size());

    JsonNode event = events.get(0);
    assertEquals(
      "TRANSPORT",
      event.path(TntConstants.EVENT_CLASSIFICATION).path(TntConstants.EVENT_TYPE).asText());
    assertEquals(
      cbr,
      event.path("shipmentDetails").path("documentReference").path("reference").asText());
    assertEquals("2025-01-25T01:23:45Z", event.path("eventUpdatedDateTime").asText());
  }

  @Test
  void shouldFilterByRequiredBaseFilterCombination() {
    TntProducer producer = createProducer();

    ConformanceResponse response =
      producer.handleRequest(
        createGetEventsRequest(
          Map.of(
            TntQueryParameters.CBR.getParameterName(), List.of("CBR-456"),
            TntQueryParameters.ER.getParameterName(), List.of("APZU4812090"))));

    JsonNode events = response.message().body().getJsonBody().path(TntConstants.EVENTS);
    assertEquals(1, events.size());
    JsonNode event = events.get(0);
    assertEquals(
      "EQUIPMENT",
      event.path(TntConstants.EVENT_CLASSIFICATION).path(TntConstants.EVENT_TYPE).asText());
    assertEquals("CBR-456", event.path("shipmentDetails").path("documentReference").path("reference").asText());
    assertEquals("APZU4812090", event.path("equipmentDetails").path("equipmentReference").asText());
  }

  @ParameterizedTest
  @EnumSource(TntEventType.class)
  void shouldReturnEventsForEveryBaseFilterOfferedPerEventType(TntEventType eventType) {
    TntProducer producer = createProducer();

    Map<String, Collection<String>> queryParams =
      exampleQueryParams(eventType.applicableBaseFilters());
    queryParams.put(TntQueryParameters.ET.getParameterName(), List.of(eventType.name()));

    JsonNode events =
      producer
        .handleRequest(createGetEventsRequest(queryParams))
        .message()
        .body()
        .getJsonBody()
        .path(TntConstants.EVENTS);

    assertFalse(
      events.isEmpty(), () -> "No %s event matched %s".formatted(eventType, queryParams));
    assertTrue(
      StreamSupport.stream(events.spliterator(), false)
        .allMatch(
          event ->
            eventType
              .name()
              .equals(
                event
                  .path(TntConstants.EVENT_CLASSIFICATION)
                  .path(TntConstants.EVENT_TYPE)
                  .asText())));
  }

  @ParameterizedTest
  @MethodSource("requiredBaseFilterCombinations")
  void shouldReturnEventsForEveryRequiredBaseFilterCombination(
    Set<TntQueryParameters> baseFilterCombination) {
    TntProducer producer = createProducer();

    Map<String, Collection<String>> queryParams = exampleQueryParams(baseFilterCombination);

    JsonNode events =
      producer
        .handleRequest(createGetEventsRequest(queryParams))
        .message()
        .body()
        .getJsonBody()
        .path(TntConstants.EVENTS);

    assertFalse(events.isEmpty(), () -> "No event matched %s".formatted(queryParams));
  }

  private static Stream<Set<TntQueryParameters>> requiredBaseFilterCombinations() {
    return Stream.of(
      Set.of(TntQueryParameters.CBR),
      Set.of(TntQueryParameters.CBR, TntQueryParameters.ER),
      Set.of(TntQueryParameters.TDR),
      Set.of(TntQueryParameters.TDR, TntQueryParameters.ER),
      Set.of(TntQueryParameters.ER),
      Set.of(
        TntQueryParameters.CBR,
        TntQueryParameters.ET,
        TntQueryParameters.E_UDT_MIN,
        TntQueryParameters.E_UDT_MAX));
  }

  @Test
  void shouldUseTheRequestedEquipmentReferenceInTheSimulatedEvents() {
    TntProducer producer = createProducer();

    ConformanceResponse response =
      producer.handleRequest(
        createGetEventsRequest(
          Map.of(
            TntQueryParameters.ER.getParameterName(), List.of("MSKU1234567"),
            TntQueryParameters.ET.getParameterName(), List.of("EQUIPMENT"))));

    JsonNode events = response.message().body().getJsonBody().path(TntConstants.EVENTS);
    assertEquals(1, events.size());
    assertEquals(
      "MSKU1234567",
      events.get(0).path("equipmentDetails").path("equipmentReference").asText());
  }

  @Test
  void shouldMatchDocumentReferencesInBothTheMainAndTheAdditionalDocumentReferences() {
    ObjectNode responseBody = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    responseBody
      .putArray(TntConstants.EVENTS)
      .add(shipmentEvent("evt-main-reference", "BOOKING", "ABC709951"))
      .add(shipmentEvent("evt-additional-reference", "TRANSPORT_DOCUMENT", "HHL71800000"));

    assertEquals(
      List.of("evt-main-reference"),
      filteredEventIDs(responseBody, TntQueryParameters.CBR, "ABC709951"));
    assertEquals(
      List.of("evt-additional-reference"),
      filteredEventIDs(responseBody, TntQueryParameters.TDR, "HHL71800000"));
    assertEquals(
      List.of(), filteredEventIDs(responseBody, TntQueryParameters.CBR, "OTHER-BOOKING"));
  }

  private static List<String> filteredEventIDs(
    JsonNode responseBody, TntQueryParameters queryParameter, String value) {
    JsonNode filtered =
      TntEventQueryFilter.filterEvents(
        responseBody, Map.of(queryParameter.getParameterName(), List.of(value)));
    return StreamSupport.stream(filtered.path(TntConstants.EVENTS).spliterator(), false)
      .map(event -> event.path("eventID").asText())
      .toList();
  }

  private static ObjectNode shipmentEvent(
    String eventID, String documentReferenceType, String reference) {
    ObjectNode event = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    event.put("eventID", eventID);
    event.putObject(TntConstants.EVENT_CLASSIFICATION).put(TntConstants.EVENT_TYPE, "SHIPMENT");
    ObjectNode shipmentDetails = event.putObject("shipmentDetails");
    if ("BOOKING".equals(documentReferenceType)) {
      shipmentDetails
        .putObject("documentReference")
        .put("type", documentReferenceType)
        .put("reference", reference);
    } else {
      shipmentDetails
        .putArray("additionalDocumentReferences")
        .addObject()
        .put("type", documentReferenceType)
        .put("reference", reference);
    }
    return event;
  }

  private static Map<String, Collection<String>> exampleQueryParams(
    Set<TntQueryParameters> queryParameters) {
    Map<String, Collection<String>> queryParams = new LinkedHashMap<>();
    SupplyScenarioParametersAction.examplePrompt(queryParameters)
      .properties()
      .forEach(property -> queryParams.put(property.getKey(), List.of(property.getValue().asText())));
    return queryParams;
  }

  @Test
  void shouldUseNextPageFixtureWhenCursorIsPresentAndFilterStillApplies() {
    TntProducer producer = createProducer();

    ConformanceResponse response =
      producer.handleRequest(
        createGetEventsRequest(
          Map.of(
            TntQueryParameters.CURSOR.getParameterName(), List.of("next-page"),
            TntQueryParameters.ET.getParameterName(), List.of("SHIPMENT"))));

    JsonNode events = response.message().body().getJsonBody().path(TntConstants.EVENTS);
    assertEquals(1, events.size());
    assertEquals("evt-resp-ship-002", events.get(0).path("eventID").asText());
    assertFalse(response.message().headers().containsKey(TntConstants.HEADER_CURSOR_NAME));
  }

  @Test
  void shouldAutoSupplyEmptyInputForOptionalOnlyScenarioParameters() {
    CapturingTntProducer producer = createCapturingProducer();

    ObjectNode actionPrompt = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    actionPrompt.put(TntConstants.ACTION_ID, "action-1");
    actionPrompt.putArray(TntConstants.TNT_QUERY_PARAMETERS)
      .add(TntQueryParameters.CBR.getParameterName())
      .add(TntQueryParameters.TDR.getParameterName())
      .add(TntQueryParameters.ER.getParameterName());
    actionPrompt.putArray(TntConstants.OPTIONAL_TNT_QUERY_PARAMETERS)
      .add(TntQueryParameters.CBR.getParameterName())
      .add(TntQueryParameters.TDR.getParameterName())
      .add(TntQueryParameters.ER.getParameterName());

    producer.getActionPromptHandlers().get(SupplyScenarioParametersAction.class).accept(actionPrompt);

    assertEquals("action-1", producer.capturedActionId);
    assertTrue(producer.capturedInput.isEmpty());
  }

  private TntProducer createProducer() {
    PartyConfiguration partyConfiguration = new PartyConfiguration();
    partyConfiguration.setName("SandboxProducer");
    partyConfiguration.setRole("Event Producer");
    partyConfiguration.setOrchestratorUrl("http://localhost");

    CounterpartConfiguration counterpartConfiguration = new CounterpartConfiguration();
    counterpartConfiguration.setName("SandboxConsumer");
    counterpartConfiguration.setRole("Event Consumer");

    JsonNodeMap persistentMap =
      new JsonNodeMap(new MemorySortedPartitionsNonLockingMap(), "pk", "sk#");

    return new TntProducer(
      "3.0.0",
      partyConfiguration,
      counterpartConfiguration,
      persistentMap,
      null,
      Map.of());
  }

  private CapturingTntProducer createCapturingProducer() {
    PartyConfiguration partyConfiguration = new PartyConfiguration();
    partyConfiguration.setName("SandboxProducer");
    partyConfiguration.setRole("Event Producer");
    partyConfiguration.setOrchestratorUrl("http://localhost");

    CounterpartConfiguration counterpartConfiguration = new CounterpartConfiguration();
    counterpartConfiguration.setName("SandboxConsumer");
    counterpartConfiguration.setRole("Event Consumer");

    JsonNodeMap persistentMap =
      new JsonNodeMap(new MemorySortedPartitionsNonLockingMap(), "pk", "sk#");

    return new CapturingTntProducer(
      "3.0.0",
      partyConfiguration,
      counterpartConfiguration,
      persistentMap,
      Map.of());
  }

  private ConformanceRequest createGetEventsRequest(
    Map<String, ? extends Collection<String>> queryParams) {
    return new ConformanceRequest(
      "GET",
      "http://localhost/events",
      queryParams,
      new ConformanceMessage(
        "AdopterConsumer",
        "Event Consumer",
        "SandboxProducer",
        "Event Producer",
        Map.of(ConformanceParty.API_VERSION, List.of("3")),
        new ConformanceMessageBody(""),
        System.currentTimeMillis()));
  }

  private static final class CapturingTntProducer extends TntProducer {
    private String capturedActionId;
    private ObjectNode capturedInput;

    private CapturingTntProducer(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
      super(apiVersion, partyConfiguration, counterpartConfiguration, persistentMap, null, orchestratorAuthHeader);
    }

    @Override
    protected void asyncOrchestratorPostPartyInput(String actionId, ObjectNode inputObjectNode) {
      this.capturedActionId = actionId;
      this.capturedInput = inputObjectNode;
    }
  }
}

