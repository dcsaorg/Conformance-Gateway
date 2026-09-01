package org.dcsa.conformance.standards.tnt.v300.party;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.state.MemorySortedPartitionsNonLockingMap;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
}

